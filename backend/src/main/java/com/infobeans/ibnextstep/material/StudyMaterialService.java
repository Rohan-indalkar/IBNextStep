package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.course.Course;
import com.infobeans.ibnextstep.course.CourseRepository;
import com.infobeans.ibnextstep.material.dto.SchedulePublishRequest;
import com.infobeans.ibnextstep.material.dto.StudyMaterialRequest;
import com.infobeans.ibnextstep.material.dto.StudyMaterialResponse;
import com.infobeans.ibnextstep.notification.Notification;
import com.infobeans.ibnextstep.notification.NotificationRepository;
import com.infobeans.ibnextstep.notification.WebPushService;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyMaterialService {

    private static final String STORAGE_SUBFOLDER = "study-materials";

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;

    // ==================== CREATE ====================

    public StudyMaterialResponse upload(String trainerEmail, StudyMaterialRequest request, List<MultipartFile> files) {
        User trainer = getTrainer(trainerEmail);
        validate(request, files, trainer);

        StudyMaterial material = StudyMaterial.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .module(request.getModule())
                .topic(request.getTopic())
                .batchIds(request.getBatchIds() == null ? List.of() : request.getBatchIds())
                .skillType(request.getSkillType())
                .difficultyLevel(request.getDifficultyLevel())
                .contentType(request.getContentType())
                .externalUrl(request.getExternalUrl())
                .files(storeFiles(files))
                .visibleFrom(request.getVisibleFrom())
                .expiryDate(request.getExpiryDate())
                .createdByTrainerId(trainer.getId())
                .createdByTrainerName(trainer.getFirstName() + " " + trainer.getLastName())
                .downloadCount(0)
                .build();

        applyPublishOption(material, request.getPublishOption(), request.getScheduledAt());

        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_UPLOADED",
                "Uploaded study material '" + material.getTitle() + "' (" + material.getStatus() + ")");
        if (material.getStatus() == MaterialStatus.PUBLISHED) {
            notifyBatchesMaterialPublished(material);
        }

        return enrich(material);
    }

    // ==================== UPDATE ====================

    public StudyMaterialResponse update(String trainerEmail, String id, StudyMaterialRequest request, List<MultipartFile> newFiles) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        validate(request, null, trainer); // files optional on update — existing ones are kept

        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setCourseId(request.getCourseId());
        material.setModule(request.getModule());
        material.setTopic(request.getTopic());
        material.setBatchIds(request.getBatchIds() == null ? List.of() : request.getBatchIds());
        material.setSkillType(request.getSkillType());
        material.setDifficultyLevel(request.getDifficultyLevel());
        material.setContentType(request.getContentType());
        material.setExternalUrl(request.getExternalUrl());
        material.setVisibleFrom(request.getVisibleFrom());
        material.setExpiryDate(request.getExpiryDate());

        if (newFiles != null && !newFiles.isEmpty()) {
            List<StudyMaterial.MaterialFile> combined = new ArrayList<>(material.getFiles());
            combined.addAll(storeFiles(newFiles));
            material.setFiles(combined);
        }

        if (material.getContentType().isFileBased() && material.getFiles().isEmpty()
                && request.getPublishOption() != PublishOption.SAVE_AS_DRAFT) {
            throw new BadRequestException("Attach at least one file before publishing this content type");
        }

        applyPublishOption(material, request.getPublishOption(), request.getScheduledAt());

        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_UPDATED", "Updated study material '" + material.getTitle() + "'");
        if (material.getStatus() == MaterialStatus.PUBLISHED) {
            notifyBatchesMaterialPublished(material);
        }

        return enrich(material);
    }

    // ==================== DELETE ====================

    public void delete(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);

        material.getFiles().forEach(f -> fileStorageService.delete(f.getStoredPath()));
        studyMaterialRepository.delete(material);

        audit(trainer, "STUDY_MATERIAL_DELETED", "Deleted study material '" + material.getTitle() + "'");
    }

    public StudyMaterialResponse deleteFile(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);

        StudyMaterial.MaterialFile toRemove = material.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        fileStorageService.delete(toRemove.getStoredPath());
        List<StudyMaterial.MaterialFile> remaining = new ArrayList<>(material.getFiles());
        remaining.remove(toRemove);
        material.setFiles(remaining);

        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_FILE_REMOVED",
                "Removed file '" + toRemove.getOriginalFileName() + "' from '" + material.getTitle() + "'");

        return enrich(material);
    }

    // ==================== PUBLISH ACTIONS ====================

    public StudyMaterialResponse publishNow(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        ensurePublishable(material);
        material.setStatus(MaterialStatus.PUBLISHED);
        material.setScheduledAt(null);
        material.setPublishedAt(Instant.now());
        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_PUBLISHED", "Published study material '" + material.getTitle() + "'");
        notifyBatchesMaterialPublished(material);
        return enrich(material);
    }

    public StudyMaterialResponse schedule(String trainerEmail, String id, SchedulePublishRequest request) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        ensurePublishable(material);
        if (request.getScheduledAt().isBefore(Instant.now())) {
            throw new BadRequestException("scheduledAt must be in the future");
        }
        material.setStatus(MaterialStatus.SCHEDULED);
        material.setScheduledAt(request.getScheduledAt());
        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_SCHEDULED",
                "Scheduled study material '" + material.getTitle() + "' for " + request.getScheduledAt());
        return enrich(material);
    }

    public StudyMaterialResponse unpublish(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        material.setStatus(MaterialStatus.DRAFT);
        material.setScheduledAt(null);
        material.setPublishedAt(null);
        material = studyMaterialRepository.save(material);
        audit(trainer, "STUDY_MATERIAL_UNPUBLISHED", "Moved study material '" + material.getTitle() + "' back to draft");
        return enrich(material);
    }

    // ==================== READ / SEARCH ====================

    public StudyMaterialResponse getOne(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        return enrich(getOwnedOrThrow(id, trainer));
    }

    public PagedResponse<StudyMaterialResponse> search(String trainerEmail, StudyMaterialSearchCriteria criteria, Pageable pageable) {
        User trainer = getTrainer(trainerEmail);
        criteria.setCreatedByTrainerId(trainer.getId());
        var page = studyMaterialRepository.search(criteria, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::enrich).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    // ==================== DOWNLOAD ====================

    public Resource download(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        StudyMaterial.MaterialFile file = material.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        material.setDownloadCount(material.getDownloadCount() + 1);
        studyMaterialRepository.save(material);

        return fileStorageService.loadAsResource(file.getStoredPath());
    }

    public StudyMaterial.MaterialFile getFileMeta(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        StudyMaterial material = getOwnedOrThrow(id, trainer);
        return material.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    // ==================== helpers ====================

    private void validate(StudyMaterialRequest request, List<MultipartFile> files, User trainer) {
        if (trainer.getTrainerType() != null && request.getSkillType() != trainer.getTrainerType()) {
            throw new BadRequestException("Skill type must match your trainer type (" + trainer.getTrainerType() + ")");
        }
        courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));

        if (request.getBatchIds() != null) {
            for (String batchId : request.getBatchIds()) {
                if (!batchRepository.existsById(batchId)) {
                    throw new ResourceNotFoundException("Batch not found: " + batchId);
                }
            }
        }

        if (request.getExpiryDate() != null && request.getVisibleFrom() != null
                && request.getExpiryDate().isBefore(request.getVisibleFrom())) {
            throw new BadRequestException("Expiry date cannot be before the visible-from date");
        }

        ContentType type = request.getContentType();
        if (!type.isFileBased()) {
            if (request.getExternalUrl() == null || request.getExternalUrl().isBlank()) {
                throw new BadRequestException(type + " requires a URL in externalUrl");
            }
        } else if (files != null && files.isEmpty() && request.getPublishOption() != PublishOption.SAVE_AS_DRAFT) {
            throw new BadRequestException("Attach at least one file before publishing this content type");
        }

        if (request.getPublishOption() == PublishOption.SCHEDULE_PUBLISH && request.getScheduledAt() == null) {
            throw new BadRequestException("scheduledAt is required when publishOption is SCHEDULE_PUBLISH");
        }
        if (request.getPublishOption() == PublishOption.SCHEDULE_PUBLISH
                && request.getScheduledAt() != null && request.getScheduledAt().isBefore(Instant.now())) {
            throw new BadRequestException("scheduledAt must be in the future");
        }
    }

    private void ensurePublishable(StudyMaterial material) {
        if (material.getBatchIds() == null || material.getBatchIds().isEmpty()) {
            throw new BadRequestException("Assign at least one batch before publishing");
        }
        if (material.getContentType().isFileBased() && (material.getFiles() == null || material.getFiles().isEmpty())) {
            throw new BadRequestException("Attach at least one file before publishing");
        }
        if (!material.getContentType().isFileBased() && (material.getExternalUrl() == null || material.getExternalUrl().isBlank())) {
            throw new BadRequestException("Add a URL before publishing");
        }
    }

    private void applyPublishOption(StudyMaterial material, PublishOption option, Instant scheduledAt) {
        if (option == null) {
            throw new BadRequestException("publishOption is required: SAVE_AS_DRAFT, PUBLISH_NOW, or SCHEDULE_PUBLISH");
        }
        switch (option) {
            case SAVE_AS_DRAFT -> {
                material.setStatus(MaterialStatus.DRAFT);
                material.setScheduledAt(null);
            }
            case PUBLISH_NOW -> {
                ensurePublishable(material);
                material.setStatus(MaterialStatus.PUBLISHED);
                material.setScheduledAt(null);
                material.setPublishedAt(Instant.now());
            }
            case SCHEDULE_PUBLISH -> {
                ensurePublishable(material);
                material.setStatus(MaterialStatus.SCHEDULED);
                material.setScheduledAt(scheduledAt);
            }
        }
    }

    private List<StudyMaterial.MaterialFile> storeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return new ArrayList<>();
        List<StudyMaterial.MaterialFile> stored = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = fileStorageService.store(file, STORAGE_SUBFOLDER);
            stored.add(StudyMaterial.MaterialFile.builder()
                    .fileId(UUID.randomUUID().toString())
                    .originalFileName(file.getOriginalFilename())
                    .storedPath(path)
                    .fileSizeBytes(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadedAt(Instant.now())
                    .build());
        }
        return stored;
    }

    private void notifyBatchesMaterialPublished(StudyMaterial material) {
        if (material.getBatchIds() == null || material.getBatchIds().isEmpty()) return;
        Set<String> studentIds = new HashSet<>();
        for (Batch batch : batchRepository.findAllById(material.getBatchIds())) {
            if (batch.getStudentIds() != null) studentIds.addAll(batch.getStudentIds());
        }
        for (User student : userRepository.findAllById(studentIds)) {
            notify(student, "New Study Material: " + material.getTitle(),
                    "A new study material '" + material.getTitle() + "' has been published for your batch.");
        }
    }

    private void notify(User recipient, String title, String message) {
        Notification notification = Notification.builder()
                .recipientUserId(recipient.getId())
                .title(title)
                .message(message)
                .senderRole("TRAINER")
                .read(false)
                .createdAt(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        emailService.send(recipient.getEmail(), title, message);
        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", notification);
        webPushService.sendToUser(recipient.getId(), title, message);
    }

    private void audit(User trainer, String action, String details) {
        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", action, details, null);
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only trainers can manage study materials");
        }
        return user;
    }

    private StudyMaterial getOwnedOrThrow(String id, User trainer) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found: " + id));
        if (!material.getCreatedByTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only manage study materials you uploaded");
        }
        return material;
    }

    private StudyMaterialResponse enrich(StudyMaterial material) {
        var builder = StudyMaterialResponse.fromEntity(material);

        courseRepository.findById(material.getCourseId()).map(Course::getName).ifPresent(builder::courseName);

        if (material.getBatchIds() != null && !material.getBatchIds().isEmpty()) {
            Map<String, String> names = new HashMap<>();
            batchRepository.findAllById(material.getBatchIds()).forEach(b -> names.put(b.getId(), b.getName()));
            builder.batchNames(material.getBatchIds().stream().map(id -> names.getOrDefault(id, id)).toList());
        } else {
            builder.batchNames(List.of());
        }

        builder.effectiveStatus(computeEffectiveStatus(material));
        return builder.build();
    }

    private String computeEffectiveStatus(StudyMaterial material) {
        if (material.getStatus() == MaterialStatus.DRAFT) return "DRAFT";
        if (material.getStatus() == MaterialStatus.SCHEDULED) return "SCHEDULED";

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (material.getExpiryDate() != null && today.isAfter(material.getExpiryDate())) return "EXPIRED";
        if (material.getVisibleFrom() != null && today.isBefore(material.getVisibleFrom())) return "UPCOMING";
        return "PUBLISHED";
    }
}
