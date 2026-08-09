package com.infobeans.ibnextstep.notification;


import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Any authenticated role (Admin/HR/Trainer/Student) calls this once per
 * browser/device, right after the user grants notification permission on
 * the frontend. No role restriction — every role can receive push.
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(Authentication authentication, @Valid @RequestBody SubscribeRequest request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Re-subscribing with the same endpoint (e.g. browser refreshed the
        // subscription) just updates the keys rather than creating a duplicate.
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);

        subscription.setUserId(user.getId());
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(Instant.now());
        }

        pushSubscriptionRepository.save(subscription);
        return ApiResponse.success("Push notifications enabled", null);
    }

    @DeleteMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribe(@RequestParam String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        return ApiResponse.success("Push notifications disabled", null);
    }
}