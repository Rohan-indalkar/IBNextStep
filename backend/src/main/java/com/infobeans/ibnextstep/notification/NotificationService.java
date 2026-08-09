package com.infobeans.ibnextstep.notification;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.notification.dto.ComposeNotificationRequest;

import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final SimpMessagingTemplate messagingTemplate;
    // NEW — OS-level browser push, reaches the recipient even if the
    // app/tab isn't open (as long as they've granted permission once).
    private final WebPushService webPushService;

    public void compose(ComposeNotificationRequest request, String senderRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = auth != null ? auth.getName() : "system";
        User sender = userRepository.findByEmail(senderEmail).orElse(null);

        List<User> recipients = request.getAudience() != null
                ? userRepository.findByRole(request.getAudience(), org.springframework.data.domain.Pageable.unpaged()).getContent()
                : userRepository.findAll();

        for (User recipient : recipients) {
            deliverToRecipient(recipient, request.getTitle(), request.getMessage(),
                    sender != null ? sender.getId() : null, senderRole);
        }

        auditLogService.log(sender != null ? sender.getId() : null, senderEmail, senderRole,
                "NOTIFICATION_SENT", "Sent notification '" + request.getTitle() + "' to "
                        + (request.getAudience() != null ? request.getAudience().name() : "ALL")
                        + " (" + recipients.size() + " recipients)", null);
    }

   
    public void sendToUser(String recipientUserId, String title, String message, String senderRole) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientUserId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = auth != null ? auth.getName() : "system";
        User sender = userRepository.findByEmail(senderEmail).orElse(null);

        deliverToRecipient(recipient, title, message, sender != null ? sender.getId() : null, senderRole);
    }

    private void deliverToRecipient(User recipient, String title, String message, String senderUserId, String senderRole) {
        Notification notification = Notification.builder()
                .recipientUserId(recipient.getId())
                .title(title)
                .message(message)
                .senderUserId(senderUserId)
                .senderRole(senderRole)
                .read(false)
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);
        emailService.send(recipient.getEmail(), title, message);

        // In-app real-time push — delivered only while the recipient
        // has the app open in a browser tab.
        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                notification
        );

        // OS-level push — delivered even with the app/tab closed, to every
        // device/browser the recipient has subscribed on. Silent no-op if
        // they never granted permission.
        webPushService.sendToUser(recipient.getId(), title, message);
    }

    public PagedResponse<Notification> myNotifications(String userId, Pageable pageable) {
        return PagedResponse.from(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    public void markAsRead(String notificationId, String requestingUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipientUserId().equals(requestingUserId)) {
            throw new BadRequestException("You can only mark your own notifications as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }
}