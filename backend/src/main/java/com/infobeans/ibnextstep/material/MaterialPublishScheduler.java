package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Backs the "Schedule Publish" option: every minute, promote any SCHEDULED
 * study material whose scheduledAt has passed to PUBLISHED, without needing
 * the trainer's browser open or any request in flight.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaterialPublishScheduler {

    private final StudyMaterialRepository studyMaterialRepository;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelay = 60_000)
    public void publishDueMaterials() {
        List<StudyMaterial> due = studyMaterialRepository
                .findByStatusAndScheduledAtBefore(MaterialStatus.SCHEDULED, Instant.now());

        for (StudyMaterial material : due) {
            material.setStatus(MaterialStatus.PUBLISHED);
            material.setPublishedAt(Instant.now());
            material.setScheduledAt(null);
            studyMaterialRepository.save(material);

            auditLogService.log(material.getCreatedByTrainerId(), material.getCreatedByTrainerName(), "TRAINER",
                    "STUDY_MATERIAL_AUTO_PUBLISHED",
                    "Scheduled publish triggered for '" + material.getTitle() + "'", null);

            log.info("Auto-published study material {} ('{}')", material.getId(), material.getTitle());
        }
    }
}
