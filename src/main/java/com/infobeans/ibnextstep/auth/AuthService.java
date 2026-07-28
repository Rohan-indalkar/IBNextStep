package com.infobeans.ibnextstep.auth;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.auth.dto.*;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.common.util.OtpGenerator;
import com.infobeans.ibnextstep.security.JwtUtil;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    /** Step 1: validate credentials, then issue and email a 2FA OTP. */
    public void login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus().name().equals("INACTIVE")) {
            throw new BadRequestException("This account has been deactivated. Contact your administrator.");
        }

        String otp = otpGenerator.generate();
        user.setOtpCode(otp);
        user.setOtpExpiry(otpGenerator.expiryFromNow());
        user.setOtpPurpose(User.OtpPurpose.LOGIN_2FA);
        userRepository.save(user);

        emailService.sendOtp(user.getEmail(), otp, "login verification");
    }

    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        User user = getUserOrThrow(request.getEmail());
        validateOtp(user, request.getOtp(), User.OtpPurpose.LOGIN_2FA);

        clearOtp(user);
        userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        String token = jwtUtil.generateToken(user.getEmail(), claims);

        auditLogService.log(user.getId(), user.getEmail(), user.getRole().name(),
                "LOGIN", "User logged in", null);

        return new AuthResponse(token, user.getRole().name(), user.getEmail(), user.isMustChangePassword());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = getUserOrThrow(request.getEmail());

        String otp = otpGenerator.generate();
        user.setOtpCode(otp);
        user.setOtpExpiry(otpGenerator.expiryFromNow());
        user.setOtpPurpose(User.OtpPurpose.PASSWORD_RESET);
        userRepository.save(user);

        emailService.sendOtp(user.getEmail(), otp, "password reset");
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = getUserOrThrow(request.getEmail());
        validateOtp(user, request.getOtp(), User.OtpPurpose.PASSWORD_RESET);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        clearOtp(user);
        userRepository.save(user);

        auditLogService.log(user.getId(), user.getEmail(), user.getRole().name(),
                "PASSWORD_RESET", "User reset their password via OTP", null);
    }

    public void logout(String userId, String userEmail, String role) {
        auditLogService.log(userId, userEmail, role, "LOGOUT", "User logged out", null);
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + email));
    }

    private void validateOtp(User user, String otp, User.OtpPurpose expectedPurpose) {
        if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
            throw new BadRequestException("No OTP requested. Please try again.");
        }
        if (user.getOtpPurpose() != expectedPurpose) {
            throw new BadRequestException("Invalid OTP request.");
        }
        if (Instant.now().isAfter(user.getOtpExpiry())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        if (!user.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP.");
        }
    }

    private void clearOtp(User user) {
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpPurpose(null);
    }
}
