package com.infobeans.ibnextstep.notification;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.notification.dto.ComposeNotificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ApiResponse<Void> compose(@Valid @RequestBody ComposeNotificationRequest request) {
        notificationService.compose(request, "ADMIN");
        return ApiResponse.success("Notification sent", null);
    }
}
