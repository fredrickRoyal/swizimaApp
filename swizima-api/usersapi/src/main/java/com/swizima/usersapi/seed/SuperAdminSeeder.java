package com.swizima.usersapi.seed;

import com.swizima.usersapi.config.SuperAdminSeedProperties;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import com.swizima.usersapi.entity.enums.UsernameType;
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

/**
 * Creates the initial super admin on start-up. Idempotent:
 * <ul>
 *     <li>does nothing if a super admin already exists;</li>
 *     <li>if a user with the configured email already exists, promotes it to {@code SUPER_ADMIN}
 *     without touching its password;</li>
 *     <li>otherwise creates the account (username = email, {@code ACTIVE}, enabled).</li>
 * </ul>
 * Configure with {@code app.seed.super-admin.*} / {@code SUPER_ADMIN_*} environment variables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements ApplicationRunner {

    private final SuperAdminSeedProperties props;
    private final SystemUserRepository userRepository;
    private final UsernameNormalizer usernameNormalizer;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.enabled()) {
            log.info("Super admin seeding disabled");
            return;
        }
        if (isBlank(props.email()) || isBlank(props.password())) {
            log.warn("Super admin seeding skipped: app.seed.super-admin.email and .password must be set");
            return;
        }
        if (userRepository.existsByRoleAndRecordStatusNot(UserRole.SUPER_ADMIN, RecordStatus.DELETED)) {
            log.debug("Super admin already exists - seeding skipped");
            return;
        }

        String email = usernameNormalizer.normalizeEmail(props.email());
        String phone = isBlank(props.phoneNumber()) ? null : usernameNormalizer.normalizePhoneNumber(props.phoneNumber());

        userRepository.findByUsernameAndRecordStatusNot(email, RecordStatus.DELETED).ifPresentOrElse(
                existing -> {
                    existing.setRole(UserRole.SUPER_ADMIN);
                    userRepository.save(existing);
                    log.info("Existing user '{}' promoted to SUPER_ADMIN", email);
                },
                () -> {
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
                            .recordStatus(RecordStatus.ACTIVE)
                            .build();
                    userRepository.save(admin);
                    log.info("Super admin '{}' created{}", email,
                            props.forcePasswordChange() ? " - password change required on first login" : "");
                });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
