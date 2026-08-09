package com.infobeans.ibnextstep.assignment;

import com.infobeans.ibnextstep.assignment.dto.AnswerRequest;
import com.infobeans.ibnextstep.assignment.dto.AssignmentFileResponse;
import com.infobeans.ibnextstep.assignment.dto.AssignmentQuestionResponse;
import com.infobeans.ibnextstep.assignment.dto.AssignmentSubmissionResponse;
import com.infobeans.ibnextstep.assignment.dto.StudentAssignmentDetailResponse;
import com.infobeans.ibnextstep.assignment.dto.StudentAssignmentListItemResponse;
import com.infobeans.ibnextstep.assignment.dto.SubmitAssignmentRequest;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.course.Course;
import com.infobeans.ibnextstep.course.CourseRepository;
import com.infobeans.ibnextstep.material.StudyMaterial;
import com.infobeans.ibnextstep.notification.Notification;
import com.infobeans.ibnextstep.notification.NotificationRepository;
import com.infobeans.ibnextstep.notification.WebPushService;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentAssignmentService {

    private static final String STORAGE_SUBFOLDER = "assignment-submissions";

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final FileStorageService fileStorageService;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;

    // ==================== BROWSE ====================

    public List<StudentAssignmentListItemResponse> listAvailable(String studentEmail) {
        User student = getStudent(studentEmail);
        List<String> batchIds = batchRepository.findByStudentIdsContaining(student.getId())
                .stream().map(Batch::getId).toList();
        if (batchIds.isEmpty()) return List.of();

        List<Assignment> assignments = assignmentRepository.search(
                        AssignmentSearchCriteria.builder().status(AssignmentStatus.PUBLISHED).build(),
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(a -> a.getBatchIds() != null && a.getBatchIds().stream().anyMatch(batchIds::contains))
                .toList();

        return assignments.stream().map(a -> {
            Optional<AssignmentSubmission> submission = submissionRepository.findByAssignmentIdAndStudentId(a.getId(), student.getId());
            String courseName = courseRepository.findById(a.getCourseId()).map(Course::getName).orElse(null);
            return StudentAssignmentListItemResponse.builder()
                    .assignmentId(a.getId()).title(a.getTitle()).courseName(courseName).topic(a.getTopic())
                    .questionCount(a.getQuestions() == null ? 0 : a.getQuestions().size())
                    .hasReferenceFiles(a.getReferenceFiles() != null && !a.getReferenceFiles().isEmpty())
                    .dueDate(a.getDueDate())
                    .submissionStatus(submission.map(AssignmentSubmission::getStatus).orElse(null))
                    .rating(submission.map(AssignmentSubmission::getRating).orElse(null))
                    .build();
        }).toList();
    }

    public StudentAssignmentDetailResponse getDetail(String studentEmail, String assignmentId) {
        User student = getStudent(studentEmail);
        Assignment assignment = getVisibleAssignment(assignmentId, student);

        List<AssignmentQuestionResponse> questions = assignment.getQuestions() == null ? List.of() : assignment.getQuestions().stream()
                .sorted((x, y) -> Integer.compare(x.getOrderIndex(), y.getOrderIndex()))
                .map(q -> AssignmentQuestionResponse.builder().id(q.getId()).questionText(q.getQuestionText()).orderIndex(q.getOrderIndex()).build())
                .toList();

        List<AssignmentFileResponse> files = assignment.getReferenceFiles() == null ? List.of() : assignment.getReferenceFiles().stream()
                .map(f -> AssignmentFileResponse.builder()
                        .fileId(f.getFileId()).fileName(f.getOriginalFileName())
                        .fileSizeBytes(f.getFileSizeBytes()).mimeType(f.getMimeType())
                        .downloadUrl("/api/student/assignments/" + assignment.getId() + "/files/" + f.getFileId() + "/download")
                        .build())
                .toList();

        AssignmentSubmissionResponse mySubmission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .map(s -> AssignmentSubmissionResponse.from(s, "/api/student/assignments/" + assignment.getId() + "/submission"))
                .orElse(null);

        String courseName = courseRepository.findById(assignment.getCourseId()).map(Course::getName).orElse(null);

        return StudentAssignmentDetailResponse.builder()
                .assignmentId(assignment.getId()).title(assignment.getTitle()).description(assignment.getDescription())
                .courseName(courseName).topic(assignment.getTopic())
                .questions(questions).referenceFiles(files).dueDate(assignment.getDueDate())
                .mySubmission(mySubmission)
                .build();
    }

    // ==================== SUBMIT ====================

    public AssignmentSubmissionResponse submit(String studentEmail, String assignmentId, SubmitAssignmentRequest request, List<MultipartFile> files) {
        User student = getStudent(studentEmail);
        Assignment assignment = getVisibleAssignment(assignmentId, student);

        Optional<AssignmentSubmission> existing = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId());
        if (existing.isPresent() && existing.get().getStatus() == AssignmentSubmissionStatus.GRADED) {
            throw new BadRequestException("This assignment has already been graded and can no longer be resubmitted");
        }

        boolean hasAnyText = request.getAnswers() != null && request.getAnswers().stream()
                .anyMatch(a -> a.getTextAnswer() != null && !a.getTextAnswer().isBlank());
        boolean hasFiles = files != null && !files.isEmpty();
        if (!hasAnyText && !hasFiles) {
            throw new BadRequestException("Provide a text answer and/or upload a file");
        }

        List<AssignmentSubmission.AnswerEntry> answers = new ArrayList<>();
        if (request.getAnswers() != null) {
            for (AnswerRequest ar : request.getAnswers()) {
                if (ar.getTextAnswer() == null || ar.getTextAnswer().isBlank()) continue;
                answers.add(AssignmentSubmission.AnswerEntry.builder()
                        .questionId(ar.getQuestionId())
                        .textAnswer(ar.getTextAnswer())
                        .files(List.of())
                        .build());
            }
        }
        if (hasFiles) {
            answers.add(AssignmentSubmission.AnswerEntry.builder()
                    .questionId(null)
                    .textAnswer(null)
                    .files(storeFiles(files))
                    .build());
        }

        String batchId = batchRepository.findByStudentIdsContaining(student.getId()).stream()
                .filter(b -> assignment.getBatchIds().contains(b.getId())).findFirst().map(Batch::getId).orElse(null);

        boolean late = assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate());

        AssignmentSubmission submission = existing.orElse(AssignmentSubmission.builder()
                .assignmentId(assignmentId).studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .batchId(batchId)
                .build());
        submission.setAnswers(answers);
        submission.setStatus(late ? AssignmentSubmissionStatus.LATE : AssignmentSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(Instant.now());

        submission = submissionRepository.save(submission);

        User trainer = userRepository.findById(assignment.getCreatedByTrainerId()).orElse(null);
        if (trainer != null) {
            notify(trainer, "New Submission: " + assignment.getTitle(),
                    submission.getStudentName() + " submitted " + (late ? "(late) " : "") + "for '" + assignment.getTitle() + "'.");
        }

        return AssignmentSubmissionResponse.from(submission, "/api/student/assignments/" + assignmentId + "/submission");
    }

    private void notify(User recipient, String title, String message) {
        Notification notification = Notification.builder()
                .recipientUserId(recipient.getId())
                .title(title)
                .message(message)
                .senderRole("SYSTEM")
                .read(false)
                .createdAt(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        emailService.send(recipient.getEmail(), title, message);
        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", notification);
        webPushService.sendToUser(recipient.getId(), title, message);
    }

    public Resource downloadReferenceFile(String studentEmail, String assignmentId, String fileId) {
        User student = getStudent(studentEmail);
        Assignment assignment = getVisibleAssignment(assignmentId, student);
        StudyMaterial.MaterialFile file = assignment.getReferenceFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        return fileStorageService.loadAsResource(file.getStoredPath());
    }

    public StudyMaterial.MaterialFile getReferenceFileMeta(String studentEmail, String assignmentId, String fileId) {
        User student = getStudent(studentEmail);
        Assignment assignment = getVisibleAssignment(assignmentId, student);
        return assignment.getReferenceFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    // ==================== helpers ====================

    private List<StudyMaterial.MaterialFile> storeFiles(List<MultipartFile> files) {
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

    private Assignment getVisibleAssignment(String assignmentId, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED && assignment.getStatus() != AssignmentStatus.CLOSED) {
            throw new BadRequestException("This assignment is not currently available");
        }
        List<Batch> studentBatches = batchRepository.findByStudentIdsContaining(student.getId());
        boolean inAssignedBatch = studentBatches.stream().anyMatch(b -> assignment.getBatchIds() != null && assignment.getBatchIds().contains(b.getId()));
        if (!inAssignedBatch) {
            throw new BadRequestException("This assignment is not assigned to your batch");
        }
        return assignment;
    }

    private User getStudent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can view or submit assignments");
        }
        return user;
    }
}
