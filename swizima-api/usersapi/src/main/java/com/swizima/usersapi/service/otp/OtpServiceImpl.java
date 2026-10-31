package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.config.OtpProperties;
import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.entity.OtpChallenge;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.OtpPurpose;
import com.swizima.usersapi.entity.enums.OtpStatus;
import com.swizima.usersapi.exception.BadRequestException;
import com.swizima.usersapi.exception.InvalidCredentialsException;
import com.swizima.usersapi.exception.OtpDeliveryException;
import com.swizima.usersapi.exception.TooManyRequestsException;
import com.swizima.usersapi.repository.OtpChallengeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private static final String INVALID_OTP = "Invalid or expired verification code";

    /** Fallback order when the default channel has no destination for this user. */
    private static final List<OtpChannel> FALLBACK_ORDER =
            List.of(OtpChannel.EMAIL, OtpChannel.SMS, OtpChannel.WHATSAPP);

    private final OtpChallengeRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties props;
    private final Map<OtpChannel, OtpSender> senders = new EnumMap<>(OtpChannel.class);
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(OtpChallengeRepository otpRepository,
                          PasswordEncoder passwordEncoder,
                          OtpProperties props,
                          List<OtpSender> senderList) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        senderList.forEach(s -> senders.put(s.channel(), s));
    }

    @Override
    @Transactional
    public OtpChallengeResponse issue(SystemUser user, OtpPurpose purpose, OtpChannel requestedChannel) {
        OtpChannel channel = resolveChannel(user, requestedChannel);
        return createAndSend(user, purpose, channel);
    }

    @Override
    @Transactional
    public OtpChallengeResponse resend(UUID otpReference, OtpPurpose purpose, OtpChannel requestedChannel) {
        OtpChallenge previous = otpRepository.findByIdAndPurpose(otpReference, purpose)
                .orElseThrow(() -> new BadRequestException("Unknown OTP reference. Please log in again."));

        if (previous.getStatus() != OtpStatus.PENDING) {
            throw new BadRequestException("This verification request is no longer active. Please log in again.");
        }

        LocalDateTime resendAvailableAt = previous.getCreatedAt().plusSeconds(props.resendCooldownSeconds());
        if (LocalDateTime.now().isBefore(resendAvailableAt)) {
            throw new TooManyRequestsException(
                    "Please wait before requesting a new code. Try again after " + resendAvailableAt);
        }

        SystemUser user = previous.getUser();
        OtpChannel channel = requestedChannel != null
                ? resolveChannel(user, requestedChannel)
                : previous.getChannel();
        return createAndSend(user, purpose, channel);
    }

    @Override
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public SystemUser verify(UUID otpReference, OtpPurpose purpose, String code) {
        OtpChallenge challenge = otpRepository.findByIdAndPurpose(otpReference, purpose)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_OTP));

        if (challenge.getStatus() != OtpStatus.PENDING || challenge.isExpired()) {
            throw new InvalidCredentialsException(INVALID_OTP);
        }

        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
                challenge.setStatus(OtpStatus.EXHAUSTED);
                otpRepository.save(challenge);
                throw new InvalidCredentialsException("Too many incorrect attempts. Please log in again.");
            }
            otpRepository.save(challenge);
            int left = challenge.getMaxAttempts() - challenge.getAttempts();
            throw new InvalidCredentialsException(INVALID_OTP + ". " + left + " attempt(s) remaining.");
        }

        challenge.setStatus(OtpStatus.VERIFIED);
        challenge.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(challenge);
        return challenge.getUser();
    }

    // ------------------------------------------------------------------

    private OtpChallengeResponse createAndSend(SystemUser user, OtpPurpose purpose, OtpChannel channel) {
        String destination = destinationFor(user, channel);
        OtpSender sender = senders.get(channel);
        if (sender == null) {
            throw new OtpDeliveryException(channel + " delivery is not supported");
        }

        // Only the newest code should be usable
        otpRepository.updateStatusForUser(user.getId(), purpose, OtpStatus.PENDING, OtpStatus.INVALIDATED);

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(props.expiryMinutes());

        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setPurpose(purpose);
        challenge.setChannel(channel);
        challenge.setDestination(destination);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(expiresAt);
        challenge.setMaxAttempts(props.maxAttempts());
        challenge.setStatus(OtpStatus.PENDING);
        challenge = otpRepository.save(challenge);

        if (props.logCode()) {
            log.warn("[DEV ONLY] OTP for {} via {}: {}", OtpMasking.mask(destination), channel, code);
        }

        // Throws OtpDeliveryException on failure, rolling back the challenge
        sender.send(destination, code, props.expiryMinutes());

        String masked = OtpMasking.mask(destination);
        LocalDateTime now = LocalDateTime.now();
        return new OtpChallengeResponse(
                challenge.getId(),
                channel,
                masked,
                expiresAt,
                now.plusSeconds(props.resendCooldownSeconds()),
                Math.max(0, java.time.Duration.between(now, expiresAt).getSeconds()),
                props.resendCooldownSeconds(),
                "A verification code has been sent via " + channel.name().toLowerCase() + " to " + masked
        );
    }

    /**
     * Explicit channel -> must have a destination, otherwise 400.
     * No channel -> user's preferred channel, then the configured default, then the first channel
     * the user has a destination for.
     */
    private OtpChannel resolveChannel(SystemUser user, OtpChannel requested) {
        if (requested != null) {
            if (destinationOrNull(user, requested) == null) {
                throw new BadRequestException(requested == OtpChannel.EMAIL
                        ? "No email address is registered for this account. Choose SMS or WHATSAPP."
                        : "No phone number is registered for this account. Choose EMAIL.");
            }
            return requested;
        }

        OtpChannel preferred = user.getPreferredOtpChannel() != null
                ? user.getPreferredOtpChannel()
                : props.defaultChannel();
        if (destinationOrNull(user, preferred) != null) {
            return preferred;
        }
        return FALLBACK_ORDER.stream()
                .filter(c -> destinationOrNull(user, c) != null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No contact details available to send a verification code"));
    }

    private String destinationFor(SystemUser user, OtpChannel channel) {
        String destination = destinationOrNull(user, channel);
        if (destination == null) {
            throw new BadRequestException("No destination registered for channel " + channel);
        }
        return destination;
    }

    private String destinationOrNull(SystemUser user, OtpChannel channel) {
        String value = switch (channel) {
            case EMAIL -> user.getEmail();
            case SMS, WHATSAPP -> user.getPhoneNumber();
        };
        return (value == null || value.isBlank()) ? null : value;
    }

    private String generateCode() {
        int length = Math.max(4, Math.min(props.length(), 8));
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
