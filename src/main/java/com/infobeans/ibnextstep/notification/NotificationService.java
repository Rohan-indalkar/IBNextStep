package com.infobeans.ibnextstep.notification;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.notification.dto.ComposeNotificationRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    public void compose(ComposeNotificationRequest request, String senderRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = auth != null ? auth.getName() : "system";
        User sender = userRepository.findByEmail(senderEmail).orElse(null);

        List<User> recipients = request.getAudience() != null
                ? userRepository.findByRole(request.getAudience(), org.springframework.data.domain.Pageable.unpaged()).getContent()
                : userRepository.findAll();

        for (User recipient : recipients) {
            Notification notification = Notification.builder()
                    .recipientUserId(recipient.getId())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .senderUserId(sender != null ? sender.getId() : null)
                    .senderRole(senderRole)
                    .read(false)
                    .createdAt(Instant.now())
                    .build();
            notificationRepository.save(notification);
            emailService.send(recipient.getEmail(), request.getTitle(), request.getMessage());
        }

        auditLogService.log(sender != null ? sender.getId() : null, senderEmail, senderRole,
                "NOTIFICATION_SENT", "Sent notification '" + request.getTitle() + "' to "
                        + (request.getAudience() != null ? request.getAudience().name() : "ALL")
                        + " (" + recipients.size() + " recipients)", null);
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
