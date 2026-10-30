package com.swizima.usersapi.service;

import com.swizima.usersapi.dto.ChangePasswordRequest;
import com.swizima.usersapi.dto.LoginRequest;
import com.swizima.usersapi.dto.LoginResponse;
import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.dto.ResendOtpRequest;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.VerifyOtpRequest;

public interface AuthService {

    /** Step 1: verifies username/password and sends an OTP. */
    OtpChallengeResponse login(LoginRequest request);

    /** Step 2: verifies the OTP and completes login. */
    LoginResponse verifyOtp(VerifyOtpRequest request);

    OtpChallengeResponse resendOtp(ResendOtpRequest request);

    SystemUserResponse changePassword(ChangePasswordRequest request);
}
