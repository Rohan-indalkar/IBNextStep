package com.infobeans.ibnextstep.notification.dto;

import com.infobeans.ibnextstep.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComposeNotificationRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String message;

    /** Null means "All" audiences. */
    private Role audience;
}
