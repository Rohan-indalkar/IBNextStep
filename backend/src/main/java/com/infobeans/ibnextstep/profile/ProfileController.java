package com.infobeans.ibnextstep.profile;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.profile.dto.ChangePasswordRequest;
import com.infobeans.ibnextstep.profile.dto.UpdateProfileRequest;
import com.infobeans.ibnextstep.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<UserResponse> getProfile(Authentication authentication) {
        return ApiResponse.success(profileService.getProfile(authentication.getName()));
    }

    @PutMapping
    public ApiResponse<UserResponse> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated", profileService.updateProfile(authentication.getName(), request));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(authentication.getName(), request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @PatchMapping("/two-factor")
    public ApiResponse<UserResponse> setTwoFactor(Authentication authentication, @RequestParam boolean enabled) {
        return ApiResponse.success(profileService.setTwoFactor(authentication.getName(), enabled));
    }
}
