package com.swizima.usersapi.service.impl;

import com.swizima.usersapi.dto.ChangePasswordRequest;
import com.swizima.usersapi.dto.LoginRequest;
import com.swizima.usersapi.dto.LoginResponse;
import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.dto.OtpLoginRequest;
import com.swizima.usersapi.dto.ResendOtpRequest;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.VerifyOtpRequest;
import com.swizima.usersapi.entity.ClientUser;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.AccountCategory;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpPurpose;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import com.swizima.usersapi.exception.AccountAccessDeniedException;
import com.swizima.usersapi.exception.BadRequestException;
import com.swizima.usersapi.exception.InvalidCredentialsException;
import com.swizima.usersapi.repository.ClientUserRepository;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.service.AuthService;
import com.swizima.usersapi.service.otp.OtpService;
import com.swizima.usersapi.util.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";
    private static final String NO_ACCOUNT = "We couldn't send a sign-in code to that email or phone number";

    private final SystemUserRepository userRepository;
    private final ClientUserRepository clientUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameNormalizer usernameNormalizer;
    private final OtpService otpService;

    @Override
    @Transactional
    public OtpChallengeResponse login(LoginRequest request) {
        SystemUser user = authenticate(request.username(), request.password());
        assertCategoryAllowed(user, request.accountCategory());
        return otpService.issue(user, OtpPurpose.LOGIN, request.otpChannel());
    }

    @Override
    @Transactional
    public OtpChallengeResponse loginWithOtp(OtpLoginRequest request) {
        String username;
        try {
            username = usernameNormalizer.normalize(request.username()).value();
        } catch (BadRequestException ex) {
            throw new InvalidCredentialsException(NO_ACCOUNT);
        }
        SystemUser user = userRepository.findByIdentityAndRecordStatusNot(username, RecordStatus.DELETED)
                .orElseThrow(() -> new InvalidCredentialsException(NO_ACCOUNT));
        assertCanLogin(user);
        assertCategoryAllowed(user, request.accountCategory());
        return otpService.issue(user, OtpPurpose.LOGIN, request.otpChannel());
    }

    @Override
    // Failed attempts must be persisted even though an exception is thrown
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        SystemUser user = otpService.verify(request.otpReference(), OtpPurpose.LOGIN, request.code());

        // Re-check account state in case it changed between the two steps
        assertCanLogin(user);

        List<ClientUser> clientLinks = clientUserRepository.findByUser(
                user.getId(), user.getPhoneNumber(), user.getEmail(), RecordStatus.DELETED);
        for (ClientUser cu : clientLinks) {
            boolean dirty = false;
            if (cu.getSystemUserId() == null) {
                cu.setSystemUserId(user.getId());
                dirty = true;
            }
            AccountCategory expectedCuCategory = cu.getStaffRef() == null
                    ? AccountCategory.CLIENT_ADMIN : AccountCategory.USER;
            if (cu.getAccountCategory() != expectedCuCategory) {
                cu.setAccountCategory(expectedCuCategory);
                dirty = true;
            }
            if (cu.getActivationStatus() == ActivationStatus.PENDING_ACTIVATION) {
                cu.setActivationStatus(ActivationStatus.ACTIVE);
                dirty = true;
            }
            if (dirty) {
                clientUserRepository.save(cu);
            }
        }
        AccountCategory resolvedCategory = resolveCategory(user, clientLinks);
        UserRole resolvedRole = resolveRole(user, resolvedCategory);
        if (!clientLinks.isEmpty() && user.getActivationStatus() == ActivationStatus.PENDING_ACTIVATION) {
            user.setActivationStatus(ActivationStatus.ACTIVE);
        }
        user.setRole(resolvedRole);
        user.setAccountCategory(resolvedCategory);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        List<UUID> clientIds = clientLinks.stream().map(ClientUser::getClientRef).distinct().toList();
        UUID defaultClientId = clientIds.isEmpty() ? null : clientIds.get(0);

        boolean mustChange = user.isPasswordSystemGenerated() && clientLinks.isEmpty();
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getUsernameType(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                resolvedCategory,
                defaultClientId,
                clientIds,
                user.getActivationStatus(),
                mustChange,
                user.getLastLoginAt(),
                mustChange ? "Password change required before continuing" : "Login successful"
        );
    }

    @Override
    @Transactional
    public OtpChallengeResponse resendOtp(ResendOtpRequest request) {
        return otpService.resend(request.otpReference(), OtpPurpose.LOGIN, request.channel());
    }

    @Override
    @Transactional
    public SystemUserResponse changePassword(ChangePasswordRequest request) {
        SystemUser user = authenticate(request.username(), request.currentPassword());

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordSystemGenerated(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        if (user.getActivationStatus() == ActivationStatus.PENDING_ACTIVATION) {
            user.setActivationStatus(ActivationStatus.ACTIVE);
        }
        return SystemUserResponse.from(userRepository.save(user));
    }

    /**
     * Verifies credentials and account state. Unknown users and wrong passwords produce the same
     * error so the API does not reveal which usernames exist.
     */
    private SystemUser authenticate(String rawUsername, String rawPassword) {
        String username;
        try {
            username = usernameNormalizer.normalize(rawUsername).value();
        } catch (BadRequestException ex) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        SystemUser user = userRepository.findByIdentityAndRecordStatusNot(username, RecordStatus.DELETED)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }
        assertCanLogin(user);
        return user;
    }

    private void assertCanLogin(SystemUser user) {
        if (user.getRecordStatus() == RecordStatus.DELETED || !user.isEnabled()) {
            throw new AccountAccessDeniedException("Account is disabled");
        }
        if (!user.getActivationStatus().canLogin()) {
            throw new AccountAccessDeniedException(
                    "Account is " + user.getActivationStatus().name().toLowerCase().replace('_', ' '));
        }
    }

    private void assertCategoryAllowed(SystemUser user, AccountCategory requestedCategory) {
        if (requestedCategory == null) {
            return;
        }
        List<ClientUser> clientLinks = clientUserRepository.findByUser(
                user.getId(), user.getPhoneNumber(), user.getEmail(), RecordStatus.DELETED);
        AccountCategory actual = resolveCategory(user, clientLinks);
        if (requestedCategory == AccountCategory.SUPER_ADMIN && actual != AccountCategory.SUPER_ADMIN) {
            throw new AccountAccessDeniedException(
                    "This is not a Super Admin account. Please select 'Client Admin' or 'User' to sign in.");
        }
        if ((requestedCategory == AccountCategory.CLIENT_ADMIN || requestedCategory == AccountCategory.USER)
                && actual == AccountCategory.SUPER_ADMIN && clientLinks.isEmpty()) {
            throw new AccountAccessDeniedException(
                    "This is a Super Admin account. Please select 'Super Admin' to sign in.");
        }
    }

    private AccountCategory resolveCategory(SystemUser user, List<ClientUser> clientLinks) {
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN) {
            return AccountCategory.SUPER_ADMIN;
        }
        if (user.getRole() == UserRole.CLIENT_ADMIN) {
            return AccountCategory.CLIENT_ADMIN;
        }
        if (!clientLinks.isEmpty()) {
            boolean hasOwnerLink = clientLinks.stream().anyMatch(cu -> cu.getStaffRef() == null);
            return hasOwnerLink ? AccountCategory.CLIENT_ADMIN : AccountCategory.USER;
        }
        return user.getAccountCategory() != null ? user.getAccountCategory() : AccountCategory.USER;
    }

    private UserRole resolveRole(SystemUser user, AccountCategory resolvedCategory) {
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.ADMIN) {
            return UserRole.SUPER_ADMIN;
        }
        return switch (resolvedCategory) {
            case SUPER_ADMIN -> UserRole.SUPER_ADMIN;
            case CLIENT_ADMIN -> UserRole.CLIENT_ADMIN;
            case USER -> UserRole.USER;
        };
    }
}
