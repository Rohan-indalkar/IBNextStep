package com.infobeans.ibnextstep.profile;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.profile.dto.ChangePasswordRequest;
import com.infobeans.ibnextstep.profile.dto.UpdateProfileRequest;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import com.infobeans.ibnextstep.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserResponse getProfile(String email) {
        return UserResponse.from(getOrThrow(email));
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getOrThrow(email);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getOrThrow(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), user.getRole().name(),
                "PASSWORD_CHANGED", "User changed their own password", null);
    }

    public UserResponse setTwoFactor(String email, boolean enabled) {
        User user = getOrThrow(email);
        user.setTwoFactorEnabled(enabled);
        user = userRepository.save(user);
        auditLogService.log(user.getId(), user.getEmail(), user.getRole().name(),
                enabled ? "TWO_FACTOR_ENABLED" : "TWO_FACTOR_DISABLED", "User toggled 2FA", null);
        return UserResponse.from(user);
    }

    private User getOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
