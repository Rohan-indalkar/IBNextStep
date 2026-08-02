package com.infobeans.ibnextstep.assignment;

import com.infobeans.ibnextstep.assignment.ai.AssignmentAiService;
import com.infobeans.ibnextstep.assignment.dto.AssignmentQuestionRequest;
import com.infobeans.ibnextstep.assignment.dto.AssignmentResponse;
import com.infobeans.ibnextstep.assignment.dto.AssignmentSubmissionResponse;
import com.infobeans.ibnextstep.assignment.dto.AssignmentSummaryResponse;
import com.infobeans.ibnextstep.assignment.dto.CreateAssignmentRequest;
import com.infobeans.ibnextstep.assignment.dto.GenerateAssignmentRequest;
import com.infobeans.ibnextstep.assignment.dto.GradeSubmissionRequest;
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
import com.infobeans.ibnextstep.material.PublishOption;
import com.infobeans.ibnextstep.material.StudyMaterial;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final String STORAGE_SUBFOLDER = "assignments";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final AssignmentAiService assignmentAiService;
    private final FileStorageService fileStorageService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;
    private final AuditLogService auditLogService;

    // ==================== AI GENERATION ====================

    /** "Create assignment through AI" — always lands as a DRAFT for review before publishing. */
    public AssignmentResponse generateWithAi(String trainerEmail, GenerateAssignmentRequest request) {
        User trainer = getTrainer(trainerEmail);
        courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + request.getCourseId()));
        if (trainer.getTrainerType() != null && request.getSkillType() != trainer.getTrainerType()) {
            throw new BadRequestException("Skill type must match your trainer type (" + trainer.getTrainerType() + ")");
        }

        List<AssignmentQuestion> questions = assignmentAiService.generateQuestions(
                request.getTopic(), request.getModule(), request.getDifficultyLevel(),
                request.getNumberOfQuestions(), request.getAdditionalInstructions());

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .module(request.getModule())
                .topic(request.getTopic())
                .skillType(request.getSkillType())
                .difficultyLevel(request.getDifficultyLevel())
                .batchIds(request.getBatchIds() == null ? List.of() : request.getBatchIds())
                .questions(questions)
                .dueDate(request.getDueDate())
                .generatedByAI(true)
                .aiPrompt("topic=" + request.getTopic() + "; difficulty=" + request.getDifficultyLevel()
                        + "; count=" + request.getNumberOfQuestions())
                .status(AssignmentStatus.DRAFT)
                .createdByTrainerId(trainer.getId())
                .createdByTrainerName(trainer.getFirstName() + " " + trainer.getLastName())
                .build();

        assignment = assignmentRepository.save(assignment);
        audit(trainer, "ASSIGNMENT_AI_GENERATED", "Generated assignment '" + assignment.getTitle() + "' via Gemini (" + questions.size() + " questions)");
        return enrich(assignment);
    }

    // ==================== CREATE / EDIT ====================

    public AssignmentResponse create(String trainerEmail, CreateAssignmentRequest request, List<MultipartFile> files) {
        User trainer = getTrainer(trainerEmail);
        validate(request, trainer);

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .courseId(request.getCourseId())
                .module(request.getModule())
                .topic(request.getTopic())
                .skillType(request.getSkillType())
                .difficultyLevel(request.getDifficultyLevel())
                .batchIds(request.getBatchIds() == null ? List.of() : request.getBatchIds())
                .questions(buildQuestionList(request.getQuestions()))
                .referenceFiles(storeFiles(files))
                .dueDate(request.getDueDate())
                .createdByTrainerId(trainer.getId())
                .createdByTrainerName(trainer.getFirstName() + " " + trainer.getLastName())
                .build();

        applyPublishOption(assignment, request.getPublishOption(), request.getScheduledAt());

        assignment = assignmentRepository.save(assignment);
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED) notifyBatchesAssignmentPublished(assignment);

        audit(trainer, "ASSIGNMENT_CREATED", "Created assignment '" + assignment.getTitle() + "' (" + assignment.getStatus() + ")");
        return enrich(assignment);
    }

    public AssignmentResponse update(String trainerEmail, String id, CreateAssignmentRequest request, List<MultipartFile> newFiles) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);

        if (assignment.getStatus() != AssignmentStatus.DRAFT && assignment.getStatus() != AssignmentStatus.SCHEDULED) {
            throw new BadRequestException("Only a DRAFT or SCHEDULED assignment can be edited — close it first if it's already live");
        }
        validate(request, trainer);

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setCourseId(request.getCourseId());
        assignment.setModule(request.getModule());
        assignment.setTopic(request.getTopic());
        assignment.setSkillType(request.getSkillType());
        assignment.setDifficultyLevel(request.getDifficultyLevel());
        assignment.setBatchIds(request.getBatchIds() == null ? List.of() : request.getBatchIds());
        assignment.setQuestions(buildQuestionList(request.getQuestions()));
        assignment.setDueDate(request.getDueDate());

        if (newFiles != null && !newFiles.isEmpty()) {
            List<StudyMaterial.MaterialFile> combined = new ArrayList<>(assignment.getReferenceFiles());
            combined.addAll(storeFiles(newFiles));
            assignment.setReferenceFiles(combined);
        }

        applyPublishOption(assignment, request.getPublishOption(), request.getScheduledAt());

        assignment = assignmentRepository.save(assignment);
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED) notifyBatchesAssignmentPublished(assignment);

        audit(trainer, "ASSIGNMENT_UPDATED", "Updated assignment '" + assignment.getTitle() + "'");
        return enrich(assignment);
    }

    public void delete(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);

        if (!submissionRepository.findByAssignmentId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete an assignment that already has student submissions — close it instead");
        }
        assignment.getReferenceFiles().forEach(f -> fileStorageService.delete(f.getStoredPath()));
        assignmentRepository.delete(assignment);
        audit(trainer, "ASSIGNMENT_DELETED", "Deleted assignment '" + assignment.getTitle() + "'");
    }

    public AssignmentResponse deleteFile(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);

        StudyMaterial.MaterialFile toRemove = assignment.getReferenceFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        fileStorageService.delete(toRemove.getStoredPath());
        List<StudyMaterial.MaterialFile> remaining = new ArrayList<>(assignment.getReferenceFiles());
        remaining.remove(toRemove);
        assignment.setReferenceFiles(remaining);

        assignment = assignmentRepository.save(assignment);
        audit(trainer, "ASSIGNMENT_FILE_REMOVED", "Removed file '" + toRemove.getOriginalFileName() + "' from '" + assignment.getTitle() + "'");
        return enrich(assignment);
    }

    // ==================== PUBLISH OPTIONS ====================

    public AssignmentResponse publishNow(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        ensurePublishable(assignment);
        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setScheduledAt(null);
        assignment.setPublishedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);
        notifyBatchesAssignmentPublished(assignment);
        audit(trainer, "ASSIGNMENT_PUBLISHED", "Published assignment '" + assignment.getTitle() + "'");
        return enrich(assignment);
    }

    public AssignmentResponse schedule(String trainerEmail, String id, Instant scheduledAt) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        ensurePublishable(assignment);
        if (scheduledAt.isBefore(Instant.now())) {
            throw new BadRequestException("scheduledAt must be in the future");
        }
        assignment.setStatus(AssignmentStatus.SCHEDULED);
        assignment.setScheduledAt(scheduledAt);
        assignment = assignmentRepository.save(assignment);
        audit(trainer, "ASSIGNMENT_SCHEDULED", "Scheduled assignment '" + assignment.getTitle() + "' for " + scheduledAt);
        return enrich(assignment);
    }

    public AssignmentResponse close(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BadRequestException("Only a PUBLISHED assignment can be closed");
        }
        assignment.setStatus(AssignmentStatus.CLOSED);
        assignment = assignmentRepository.save(assignment);
        audit(trainer, "ASSIGNMENT_CLOSED", "Closed assignment '" + assignment.getTitle() + "'");
        return enrich(assignment);
    }

    // ==================== READ / DASHBOARD ====================

    public AssignmentResponse getOne(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        return enrich(getOwnedOrThrow(id, trainer));
    }

    public PagedResponse<AssignmentSummaryResponse> dashboard(String trainerEmail, AssignmentSearchCriteria criteria, Pageable pageable) {
        User trainer = getTrainer(trainerEmail);
        criteria.setCreatedByTrainerId(trainer.getId());
        var page = assignmentRepository.search(criteria, pageable);

        List<AssignmentSummaryResponse> rows = page.getContent().stream().map(a -> {
            List<AssignmentSubmission> submissions = submissionRepository.findByAssignmentId(a.getId());
            long graded = submissions.stream().filter(s -> s.getStatus() == AssignmentSubmissionStatus.GRADED).count();
            String courseName = courseRepository.findById(a.getCourseId()).map(Course::getName).orElse(null);

            return AssignmentSummaryResponse.builder()
                    .id(a.getId()).title(a.getTitle()).courseName(courseName).status(a.getStatus())
                    .questionCount(a.getQuestions() == null ? 0 : a.getQuestions().size())
                    .generatedByAI(a.isGeneratedByAI())
                    .dueDate(a.getDueDate()).publishedAt(a.getPublishedAt())
                    .totalSubmissions(submissions.size()).gradedSubmissions(graded)
                    .build();
        }).toList();

        return new PagedResponse<>(rows, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    // ==================== SUBMISSIONS / GRADING ====================

    public List<AssignmentSubmissionResponse> submissions(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        return submissionRepository.findByAssignmentId(assignment.getId()).stream()
                .map(s -> AssignmentSubmissionResponse.from(s,
                        "/api/trainer/assignments/" + assignment.getId() + "/submissions/" + s.getStudentId()))
                .toList();
    }

    public AssignmentSubmissionResponse submissionDetail(String trainerEmail, String id, String studentId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        AssignmentSubmission submission = getSubmissionOrThrow(assignment.getId(), studentId);
        return AssignmentSubmissionResponse.from(submission,
                "/api/trainer/assignments/" + assignment.getId() + "/submissions/" + studentId);
    }

    /** Practice feedback — written comments + 1-5 rating, no numeric score. */
    public AssignmentSubmissionResponse grade(String trainerEmail, String id, String studentId, GradeSubmissionRequest request) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        AssignmentSubmission submission = getSubmissionOrThrow(assignment.getId(), studentId);

        submission.setFeedback(request.getFeedback());
        submission.setRating(request.getRating());
        submission.setStatus(AssignmentSubmissionStatus.GRADED);
        submission.setGradedAt(Instant.now());
        submission.setGradedByTrainerId(trainer.getId());
        submission = submissionRepository.save(submission);

        User student = userRepository.findById(studentId).orElse(null);
        if (student != null) {
            notify(student, "Feedback Ready: " + assignment.getTitle(),
                    "Your trainer reviewed your submission for '" + assignment.getTitle() + "'. Rating: "
                            + request.getRating() + "/5. " + (request.getFeedback() != null ? request.getFeedback() : ""));
        }

        audit(trainer, "ASSIGNMENT_SUBMISSION_GRADED", "Gave feedback on '" + assignment.getTitle() + "' for student " + studentId);
        return AssignmentSubmissionResponse.from(submission, "/api/trainer/assignments/" + assignment.getId() + "/submissions/" + studentId);
    }

    // ==================== DOWNLOADS ====================

    public Resource downloadReferenceFile(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        StudyMaterial.MaterialFile file = findReferenceFile(assignment, fileId);
        return fileStorageService.loadAsResource(file.getStoredPath());
    }

    public StudyMaterial.MaterialFile getReferenceFileMeta(String trainerEmail, String id, String fileId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        return findReferenceFile(assignment, fileId);
    }

    public Resource downloadSubmissionFile(String trainerEmail, String id, String studentId, String fileId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        AssignmentSubmission submission = getSubmissionOrThrow(assignment.getId(), studentId);
        StudyMaterial.MaterialFile file = findSubmissionFile(submission, fileId);
        return fileStorageService.loadAsResource(file.getStoredPath());
    }

    public StudyMaterial.MaterialFile getSubmissionFileMeta(String trainerEmail, String id, String studentId, String fileId) {
        User trainer = getTrainer(trainerEmail);
        Assignment assignment = getOwnedOrThrow(id, trainer);
        AssignmentSubmission submission = getSubmissionOrThrow(assignment.getId(), studentId);
        return findSubmissionFile(submission, fileId);
    }

    // ==================== helpers ====================

    private StudyMaterial.MaterialFile findReferenceFile(Assignment assignment, String fileId) {
        return assignment.getReferenceFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    private StudyMaterial.MaterialFile findSubmissionFile(AssignmentSubmission submission, String fileId) {
        return submission.getAnswers().stream()
                .flatMap(a -> a.getFiles() == null ? java.util.stream.Stream.empty() : a.getFiles().stream())
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    private AssignmentSubmission getSubmissionOrThrow(String assignmentId, String studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No submission from this student yet"));
    }

    private List<AssignmentQuestion> buildQuestionList(List<AssignmentQuestionRequest> requests) {
        List<AssignmentQuestion> questions = new ArrayList<>();
        if (requests == null) return questions;
        int order = 0;
        for (AssignmentQuestionRequest qr : requests) {
            questions.add(AssignmentQuestion.builder()
                    .id(qr.getId() != null && !qr.getId().isBlank() ? qr.getId() : UUID.randomUUID().toString())
                    .questionText(qr.getQuestionText())
                    .orderIndex(order++)
                    .build());
        }
        return questions;
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

    private void validate(CreateAssignmentRequest request, User trainer) {
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
        if (request.getPublishOption() == PublishOption.SCHEDULE_PUBLISH && request.getScheduledAt() == null) {
            throw new BadRequestException("scheduledAt is required when publishOption is SCHEDULE_PUBLISH");
        }
        if (request.getPublishOption() == PublishOption.SCHEDULE_PUBLISH
                && request.getScheduledAt() != null && request.getScheduledAt().isBefore(Instant.now())) {
            throw new BadRequestException("scheduledAt must be in the future");
        }
    }

    private void ensurePublishable(Assignment assignment) {
        if (assignment.getBatchIds() == null || assignment.getBatchIds().isEmpty()) {
            throw new BadRequestException("Assign at least one batch before publishing");
        }
        boolean hasQuestions = assignment.getQuestions() != null && !assignment.getQuestions().isEmpty();
        boolean hasFiles = assignment.getReferenceFiles() != null && !assignment.getReferenceFiles().isEmpty();
        if (!hasQuestions && !hasFiles) {
            throw new BadRequestException("Add at least one question or a reference file before publishing");
        }
    }

    private void applyPublishOption(Assignment assignment, PublishOption option, Instant scheduledAt) {
        if (option == null) {
            throw new BadRequestException("publishOption is required: SAVE_AS_DRAFT, PUBLISH_NOW, or SCHEDULE_PUBLISH");
        }
        switch (option) {
            case SAVE_AS_DRAFT -> {
                assignment.setStatus(AssignmentStatus.DRAFT);
                assignment.setScheduledAt(null);
            }
            case PUBLISH_NOW -> {
                ensurePublishable(assignment);
                assignment.setStatus(AssignmentStatus.PUBLISHED);
                assignment.setScheduledAt(null);
                assignment.setPublishedAt(Instant.now());
            }
            case SCHEDULE_PUBLISH -> {
                ensurePublishable(assignment);
                assignment.setStatus(AssignmentStatus.SCHEDULED);
                assignment.setScheduledAt(scheduledAt);
            }
        }
    }

    private void notifyBatchesAssignmentPublished(Assignment assignment) {
        if (assignment.getBatchIds() == null || assignment.getBatchIds().isEmpty()) return;
        Set<String> studentIds = new HashSet<>();
        for (Batch batch : batchRepository.findAllById(assignment.getBatchIds())) {
            if (batch.getStudentIds() != null) studentIds.addAll(batch.getStudentIds());
        }
        String dueText = assignment.getDueDate() != null ? (" Due: " + assignment.getDueDate() + ".") : "";
        for (User student : userRepository.findAllById(studentIds)) {
            notify(student, "New Assignment: " + assignment.getTitle(),
                    "A new practice assignment '" + assignment.getTitle() + "' is now available." + dueText);
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
            throw new BadRequestException("Only trainers can manage assignments");
        }
        return user;
    }

    private Assignment getOwnedOrThrow(String id, User trainer) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
        if (!assignment.getCreatedByTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only manage assignments you created");
        }
        return assignment;
    }

    private AssignmentResponse enrich(Assignment assignment) {
        var builder = AssignmentResponse.fromEntity(assignment);
        courseRepository.findById(assignment.getCourseId()).map(Course::getName).ifPresent(builder::courseName);
        if (assignment.getBatchIds() != null && !assignment.getBatchIds().isEmpty()) {
            Map<String, String> names = new HashMap<>();
            batchRepository.findAllById(assignment.getBatchIds()).forEach(b -> names.put(b.getId(), b.getName()));
            builder.batchNames(assignment.getBatchIds().stream().map(id -> names.getOrDefault(id, id)).toList());
        } else {
            builder.batchNames(List.of());
        }
        return builder.build();
    }
}