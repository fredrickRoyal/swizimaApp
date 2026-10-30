package com.swizima.usersapi.service.impl;

import com.swizima.usersapi.dto.ChangePasswordRequest;
import com.swizima.usersapi.dto.LoginRequest;
import com.swizima.usersapi.dto.LoginResponse;
import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.dto.ResendOtpRequest;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.VerifyOtpRequest;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpPurpose;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.exception.AccountAccessDeniedException;
import com.swizima.usersapi.exception.BadRequestException;
import com.swizima.usersapi.exception.InvalidCredentialsException;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.service.AuthService;
import com.swizima.usersapi.service.otp.OtpService;
import com.swizima.usersapi.util.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameNormalizer usernameNormalizer;
    private final OtpService otpService;

    @Override
    @Transactional
    public OtpChallengeResponse login(LoginRequest request) {
        SystemUser user = authenticate(request.username(), request.password());
        return otpService.issue(user, OtpPurpose.LOGIN, request.otpChannel());
    }

    @Override
    // Failed attempts must be persisted even though an exception is thrown
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        SystemUser user = otpService.verify(request.otpReference(), OtpPurpose.LOGIN, request.code());

        // Re-check account state in case it changed between the two steps
        assertCanLogin(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        boolean mustChange = user.isPasswordSystemGenerated();
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getUsernameType(),
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

        SystemUser user = userRepository.findByUsernameAndRecordStatusNot(username, RecordStatus.DELETED)
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
}
