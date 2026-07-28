package com.infobeans.ibnextstep.auth;

import com.infobeans.ibnextstep.auth.dto.*;
import com.infobeans.ibnextstep.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<Void> login(@Valid @RequestBody LoginRequest request) {
        authService.login(request);
        return ApiResponse.success("OTP sent to your registered email. Please verify to continue.", null);
    }

    @PostMapping("/verify-login-otp")
    public ApiResponse<AuthResponse> verifyLoginOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.success("Login successful", authService.verifyLoginOtp(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("OTP sent to your registered email.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password reset successful. Please log in with your new password.", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        authService.logout(null, auth.getName(), auth.getAuthorities().toString());
        return ApiResponse.success("Logged out successfully", null);
    }
}
