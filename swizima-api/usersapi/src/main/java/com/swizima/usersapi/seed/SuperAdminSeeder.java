package com.swizima.usersapi.seed;

import com.swizima.usersapi.config.SuperAdminSeedProperties;
import com.swizima.usersapi.entity.ClientUser;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.AccountCategory;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import com.swizima.usersapi.entity.enums.UsernameType;
import com.swizima.usersapi.repository.ClientUserRepository;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.util.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Creates the initial super admin on start-up and reconciles account roles/categories:
 * <ul>
 *     <li>Default super admin ({@code kfredrick35@gmail.com}) is ensured to have {@code SUPER_ADMIN};</li>
 *     <li>Client accounts without {@code staffRef} (created for a client) are ensured to have {@code CLIENT_ADMIN};</li>
 *     <li>Client staff accounts ({@code staffRef != null}) and regular accounts have {@code USER}.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements ApplicationRunner {

    private final SuperAdminSeedProperties props;
    private final SystemUserRepository userRepository;
    private final ClientUserRepository clientUserRepository;
    private final UsernameNormalizer usernameNormalizer;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSuperAdmin();
        reconcileClientAccountRoles();
    }

    private void seedSuperAdmin() {
        if (!props.enabled()) {
            log.info("Super admin seeding disabled");
            return;
        }
        if (isBlank(props.email()) || isBlank(props.password())) {
            log.warn("Super admin seeding skipped: app.seed.super-admin.email and .password must be set");
            return;
        }

        String email = usernameNormalizer.normalizeEmail(props.email());
        String phone = isBlank(props.phoneNumber()) ? null : usernameNormalizer.normalizePhoneNumber(props.phoneNumber());

        Optional<SystemUser> byIdentity = userRepository
                .findByAnyContact(email, email, phone, RecordStatus.DELETED)
                .stream()
                .findFirst();

        if (byIdentity.isPresent()) {
            SystemUser existing = byIdentity.get();
            boolean changed = false;
            if (existing.getRole() != UserRole.SUPER_ADMIN) {
                existing.setRole(UserRole.SUPER_ADMIN);
                changed = true;
            }
            if (existing.getAccountCategory() != AccountCategory.SUPER_ADMIN) {
                existing.setAccountCategory(AccountCategory.SUPER_ADMIN);
                changed = true;
            }
            if (changed) {
                userRepository.save(existing);
                log.info("Ensured user '{}' has SUPER_ADMIN role and category", existing.getUsername());
            }
            return;
        }

        if (userRepository.existsByRoleAndRecordStatusNot(UserRole.SUPER_ADMIN, RecordStatus.DELETED)) {
            for (SystemUser su : userRepository.findByRoleAndEnabledTrueAndRecordStatusNot(UserRole.SUPER_ADMIN, RecordStatus.DELETED)) {
                if (su.getAccountCategory() != AccountCategory.SUPER_ADMIN) {
                    su.setAccountCategory(AccountCategory.SUPER_ADMIN);
                    userRepository.save(su);
                }
            }
            return;
        }

        SystemUser admin = SystemUser.builder()
                .username(email)
                .usernameType(UsernameType.EMAIL)
                .email(email)
                .phoneNumber(phone)
                .preferredOtpChannel(OtpChannel.EMAIL)
                .password(passwordEncoder.encode(props.password()))
                .passwordSystemGenerated(props.forcePasswordChange())
                .passwordChangedAt(LocalDateTime.now())
                .enabled(true)
                .activationStatus(ActivationStatus.ACTIVE)
                .role(UserRole.SUPER_ADMIN)
                .accountCategory(AccountCategory.SUPER_ADMIN)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        log.info("Super admin '{}' created{}", email,
                props.forcePasswordChange() ? " - password change required on first login" : "");
    }

    /**
     * Ensures that client accounts created for a client (staffRef == null) have CLIENT_ADMIN,
     * and staff accounts (staffRef != null) have USER.
     */
    private void reconcileClientAccountRoles() {
        List<ClientUser> allLinks = clientUserRepository.findAll();
        for (ClientUser cu : allLinks) {
            if (cu.getRecordStatus() == RecordStatus.DELETED) {
                continue;
            }
            AccountCategory expectedCategory = cu.getStaffRef() == null
                    ? AccountCategory.CLIENT_ADMIN
                    : AccountCategory.USER;
            UserRole expectedRole = expectedCategory == AccountCategory.CLIENT_ADMIN
                    ? UserRole.CLIENT_ADMIN
                    : UserRole.USER;

            boolean cuDirty = false;
            if (cu.getAccountCategory() != expectedCategory) {
                cu.setAccountCategory(expectedCategory);
                cuDirty = true;
            }

            Optional<SystemUser> linkedUser = Optional.empty();
            if (cu.getSystemUserId() != null) {
                linkedUser = userRepository.findByIdAndRecordStatusNot(cu.getSystemUserId(), RecordStatus.DELETED);
            }
            if (linkedUser.isEmpty()) {
                String lookupKey = cu.getUserPhoneNumber() != null ? cu.getUserPhoneNumber() : cu.getUserEmail();
                if (lookupKey != null) {
                    linkedUser = userRepository.findByAnyContact(
                                    lookupKey, cu.getUserEmail(), cu.getUserPhoneNumber(), RecordStatus.DELETED)
                            .stream()
                            .findFirst();
                }
            }

            if (linkedUser.isPresent()) {
                SystemUser su = linkedUser.get();
                if (cu.getSystemUserId() == null) {
                    cu.setSystemUserId(su.getId());
                    cuDirty = true;
                }
                if (su.getRole() != UserRole.SUPER_ADMIN && su.getRole() != UserRole.ADMIN) {
                    // Promote to CLIENT_ADMIN if this link is a client owner link, or set USER if not already CLIENT_ADMIN
                    if (expectedRole == UserRole.CLIENT_ADMIN
                            || (su.getRole() != UserRole.CLIENT_ADMIN && su.getAccountCategory() != AccountCategory.CLIENT_ADMIN)) {
                        if (su.getRole() != expectedRole || su.getAccountCategory() != expectedCategory) {
                            su.setRole(expectedRole);
                            su.setAccountCategory(expectedCategory);
                            userRepository.save(su);
                            log.info("Updated client user '{}' to role={}", su.getUsername(), expectedRole);
                        }
                    }
                }
            }

            if (cuDirty) {
                clientUserRepository.save(cu);
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
