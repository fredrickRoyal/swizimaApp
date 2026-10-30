package com.swizima.usersapi.controller;

import com.swizima.usersapi.dto.ChangePasswordRequest;
import com.swizima.usersapi.dto.LoginRequest;
import com.swizima.usersapi.dto.LoginResponse;
import com.swizima.usersapi.dto.OtpChallengeResponse;
import com.swizima.usersapi.dto.ResendOtpRequest;
import com.swizima.usersapi.dto.SystemUserResponse;
import com.swizima.usersapi.dto.VerifyOtpRequest;
import com.swizima.usersapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login with OTP and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Step 1 - verify username/password and send an OTP",
            description = "Optionally set otpChannel to EMAIL, SMS or WHATSAPP. Defaults to the user's preferred "
                    + "channel (EMAIL by default). Returns an otpReference to use in /verify-otp.")
    public ResponseEntity<OtpChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Step 2 - verify the OTP and complete login",
            description = "If mustChangePassword is true in the response, the client must call "
                    + "/api/v1/auth/change-password before continuing.")
    public ResponseEntity<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend the OTP, optionally via a different channel")
    public ResponseEntity<OtpChallengeResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password",
            description = "Clears the system-generated flag and activates accounts pending activation.")
    public ResponseEntity<SystemUserResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }
}
