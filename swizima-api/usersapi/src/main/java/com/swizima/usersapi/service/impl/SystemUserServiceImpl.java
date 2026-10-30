package com.swizima.usersapi.service.impl;

import com.swizima.usersapi.dto.CreateUserRequest;
import com.swizima.usersapi.dto.PageResponse;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.UpdateUserRequest;
import com.swizima.usersapi.dto.UserCredentialsResponse;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UsernameType;
import com.swizima.usersapi.exception.DuplicateResourceException;
import com.swizima.usersapi.exception.ResourceNotFoundException;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.service.SystemUserService;
import com.swizima.usersapi.util.PasswordGenerator;
import com.swizima.usersapi.util.UsernameNormalizer;
import com.swizima.usersapi.util.UsernameNormalizer.NormalizedUsername;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemUserServiceImpl implements SystemUserService {

    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final UsernameNormalizer usernameNormalizer;

    @Override
    @Transactional
    public UserCredentialsResponse createUser(CreateUserRequest request) {
        NormalizedUsername username = usernameNormalizer.normalize(request.username());
        if (userRepository.existsByUsername(username.value())) {
            throw new DuplicateResourceException("User '" + username.value() + "' already exists");
        }

        boolean generated = request.password() == null || request.password().isBlank();
        String rawPassword = generated ? passwordGenerator.generate() : request.password();

        SystemUser user = new SystemUser();
        user.setUsername(username.value());
        user.setUsernameType(username.type());
        applyContactDetails(user, username, request.email(), request.phoneNumber(), request.preferredOtpChannel());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPasswordSystemGenerated(generated);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setEnabled(request.enabled() == null || request.enabled());
        // A user who chose their own password is active immediately; otherwise they
        // become active after replacing the temporary password.
        user.setActivationStatus(generated ? ActivationStatus.PENDING_ACTIVATION : ActivationStatus.ACTIVE);
        user.setRecordStatus(RecordStatus.ACTIVE);

        SystemUserResponse saved = SystemUserResponse.from(userRepository.save(user));
        return new UserCredentialsResponse(saved, generated ? rawPassword : null);
    }

    @Override
    @Transactional
    public SystemUserResponse updateUser(UUID id, UpdateUserRequest request) {
        SystemUser user = findActiveUser(id);

        NormalizedUsername username = usernameNormalizer.normalize(request.username());
        if (userRepository.existsByUsernameAndIdNot(username.value(), id)) {
            throw new DuplicateResourceException("User '" + username.value() + "' already exists");
        }

        user.setUsername(username.value());
        user.setUsernameType(username.type());
        applyContactDetails(user, username, request.email(), request.phoneNumber(), request.preferredOtpChannel());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return SystemUserResponse.from(userRepository.save(user));
    }

    @Override
    public SystemUserResponse getUserById(UUID id) {
        return SystemUserResponse.from(findActiveUser(id));
    }

    @Override
    public SystemUserResponse getUserByUsername(String rawUsername) {
        String username = usernameNormalizer.normalize(rawUsername).value();
        return userRepository.findByUsernameAndRecordStatusNot(username, RecordStatus.DELETED)
                .map(SystemUserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found"));
    }

    @Override
    public PageResponse<SystemUserResponse> getUsers(String search, ActivationStatus activationStatus,
                                                     Boolean enabled, Pageable pageable) {
        String term = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(
                userRepository.search(term, activationStatus, enabled, RecordStatus.DELETED, pageable)
                        .map(SystemUserResponse::from));
    }

    @Override
    @Transactional
    public SystemUserResponse updateActivationStatus(UUID id, ActivationStatus activationStatus) {
        SystemUser user = findActiveUser(id);
        user.setActivationStatus(activationStatus);
        return SystemUserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public SystemUserResponse setEnabled(UUID id, boolean enabled) {
        SystemUser user = findActiveUser(id);
        user.setEnabled(enabled);
        return SystemUserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserCredentialsResponse resetPassword(UUID id) {
        SystemUser user = findActiveUser(id);
        String tempPassword = passwordGenerator.generate();

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setPasswordSystemGenerated(true);
        user.setPasswordChangedAt(LocalDateTime.now());

        return new UserCredentialsResponse(SystemUserResponse.from(userRepository.save(user)), tempPassword);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        SystemUser user = findActiveUser(id);
        // Soft delete: keep the row for audit purposes and block access
        user.setRecordStatus(RecordStatus.DELETED);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private SystemUser findActiveUser(UUID id) {
        return userRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User with id '" + id + "' not found"));
    }

    /**
     * Sets OTP contact details. The username fills the matching contact field when it isn't supplied,
     * so a user who signs up with an email can receive email OTPs without extra input.
     */
    private void applyContactDetails(SystemUser user, NormalizedUsername username,
                                     String rawEmail, String rawPhone, OtpChannel preferredChannel) {
        String email = usernameNormalizer.normalizeEmail(rawEmail);
        String phone = usernameNormalizer.normalizePhoneNumber(rawPhone);

        if (email == null && username.type() == UsernameType.EMAIL) {
            email = username.value();
        }
        if (phone == null && username.type() == UsernameType.PHONE) {
            phone = username.value();
        }

        user.setEmail(email != null ? email : user.getEmail());
        user.setPhoneNumber(phone != null ? phone : user.getPhoneNumber());
        if (preferredChannel != null) {
            user.setPreferredOtpChannel(preferredChannel);
        } else if (user.getPreferredOtpChannel() == null) {
            user.setPreferredOtpChannel(OtpChannel.EMAIL);
        }
    }
}
