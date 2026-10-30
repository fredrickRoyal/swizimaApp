package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.OtpPurpose;

import java.util.UUID;

public interface OtpService {

    /** Generates, stores and sends a new OTP, invalidating any outstanding one for the same purpose. */
    OtpChallengeResponse issue(SystemUser user, OtpPurpose purpose, OtpChannel requestedChannel);

    /** Re-sends a fresh code for an existing challenge, optionally over a different channel. */
    OtpChallengeResponse resend(UUID otpReference, OtpPurpose purpose, OtpChannel requestedChannel);

    /** Verifies a code and returns the user it belongs to. The challenge can only be used once. */
    SystemUser verify(UUID otpReference, OtpPurpose purpose, String code);
}
