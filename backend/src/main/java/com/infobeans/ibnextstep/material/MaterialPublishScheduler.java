package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.notification.Notification;
import com.infobeans.ibnextstep.notification.NotificationRepository;
import com.infobeans.ibnextstep.notification.WebPushService;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Backs the "Schedule Publish" option: every minute, promote any SCHEDULED
 * study material whose scheduledAt has passed to PUBLISHED, without needing
 * the trainer's browser open or any request in flight — and notify every
 * student in the assigned batches (in-app + email), same as a manual publish.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaterialPublishScheduler {

    private final StudyMaterialRepository studyMaterialRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelay = 60_000)
    public void publishDueMaterials() {
        List<StudyMaterial> due = studyMaterialRepository
                .findByStatusAndScheduledAtBefore(MaterialStatus.SCHEDULED, Instant.now());

        for (StudyMaterial material : due) {
            material.setStatus(MaterialStatus.PUBLISHED);
            material.setPublishedAt(Instant.now());
            material.setScheduledAt(null);
            material = studyMaterialRepository.save(material);

            notifyBatches(material);

            auditLogService.log(material.getCreatedByTrainerId(), material.getCreatedByTrainerName(), "TRAINER",
                    "STUDY_MATERIAL_AUTO_PUBLISHED",
                    "Scheduled publish triggered for '" + material.getTitle() + "'", null);

            log.info("Auto-published study material {} ('{}')", material.getId(), material.getTitle());
        }
    }

    private void notifyBatches(StudyMaterial material) {
        if (material.getBatchIds() == null || material.getBatchIds().isEmpty()) return;
        Set<String> studentIds = new HashSet<>();
        for (Batch batch : batchRepository.findAllById(material.getBatchIds())) {
            if (batch.getStudentIds() != null) studentIds.addAll(batch.getStudentIds());
        }
        for (User student : userRepository.findAllById(studentIds)) {
            String title = "New Study Material: " + material.getTitle();
            String message = "A new study material '" + material.getTitle() + "' has been published for your batch.";

            Notification notification = Notification.builder()
                    .recipientUserId(student.getId()).title(title).message(message)
                    .senderRole("SYSTEM").read(false).createdAt(Instant.now())
                    .build();
            notification = notificationRepository.save(notification);

            emailService.send(student.getEmail(), title, message);
            messagingTemplate.convertAndSendToUser(student.getEmail(), "/queue/notifications", notification);
            webPushService.sendToUser(student.getId(), title, message);
        }
    }
}
