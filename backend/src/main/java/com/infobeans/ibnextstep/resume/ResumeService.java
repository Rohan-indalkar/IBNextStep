package com.infobeans.ibnextstep.resume;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.common.util.PdfTextExtractionService;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.resume.ai.ResumeAiService;
import com.infobeans.ibnextstep.resume.dto.ReviewResumeRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final String STORAGE_SUBFOLDER = "resumes";

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final ResumeAiService resumeAiService;

    @Value("${app.resume.ai.approval-threshold:70}")
    private int aiApprovalThreshold;

    // ==================== STUDENT SIDE ====================

    /**
     * "Upload New Version" — first upload creates the Resume doc; every
     * later upload appends a new version rather than overwriting, so the
     * full review history survives across NEEDS_CHANGES rounds.
     * If the previous version was NEEDS_CHANGES, this also fires
     * "Notification to Trainer" so they know a re-review is waiting.
     */
    public Resume uploadVersion(String studentEmail, MultipartFile file) {
        User student = getStudent(studentEmail);
        String path = fileStorageService.store(file, STORAGE_SUBFOLDER);

        Resume resume = resumeRepository.findByStudentId(student.getId()).orElse(null);
        Resume.ResumeVersion previousLatest = (resume != null && !resume.getVersions().isEmpty())
                ? resume.getVersions().get(resume.getVersions().size() - 1)
                : null;

        Resume.ResumeVersion newVersion = Resume.ResumeVersion.builder()
                .versionNumber(previousLatest != null ? previousLatest.getVersionNumber() + 1 : 1)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .uploadedAt(Instant.now())
                .status(ResumeStatus.PENDING_REVIEW)
                .build();

        if (resume == null) {
            resume = Resume.builder()
                    .studentId(student.getId())
                    .currentStatus(ResumeStatus.PENDING_REVIEW)
                    .versions(new ArrayList<>(List.of(newVersion)))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        } else {
            resume.getVersions().add(newVersion);
            resume.setCurrentStatus(ResumeStatus.PENDING_REVIEW);
            resume.setUpdatedAt(Instant.now());
        }

        resume = resumeRepository.save(resume);

        // "Notification to Trainer" — only meaningful on a re-upload after feedback, not the very first submission.
        if (previousLatest != null && previousLatest.getReviewedByTrainerId() != null) {
            notificationService.sendToUser(previousLatest.getReviewedByTrainerId(),
                    "Resume re-submitted for review",
                    student.getFirstName() + " " + student.getLastName() + " uploaded a revised resume (v"
                            + newVersion.getVersionNumber() + ") after your feedback. Please re-review.",
                    "SYSTEM");
        }

        return resume;
    }

    public Resume myResume(String studentEmail) {
        User student = getStudent(studentEmail);
        return resumeRepository.findByStudentId(student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You haven't uploaded a resume yet"));
    }

    // ==================== TRAINER SIDE ====================

    /** "Select Student" → "View Student Resume" — only works if the student is actually in one of this trainer's batches. */
    public Resume getStudentResume(String trainerEmail, String studentId) {
        User trainer = getTrainer(trainerEmail);
        verifyStudentAssignedToTrainer(trainer.getId(), studentId);

        return resumeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("This student hasn't uploaded a resume yet"));
    }

    /**
     * "Review Resume" → "Provide Suggestions" → "Assign Resume Score" →
     * "Choose Status" → "Send Notification to Student", all in one call.
     */
    public Resume review(String trainerEmail, String studentId, ReviewResumeRequest request) {
        User trainer = getTrainer(trainerEmail);
        verifyStudentAssignedToTrainer(trainer.getId(), studentId);

        Resume resume = resumeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("This student hasn't uploaded a resume yet"));

        if (resume.getVersions().isEmpty()) {
            throw new BadRequestException("This resume has no versions to review");
        }

        Resume.ResumeVersion latest = resume.getVersions().get(resume.getVersions().size() - 1);
        latest.setSuggestions(request.getSuggestions());
        latest.setScore(request.getScore());
        latest.setStatus(request.getStatus());
        latest.setReviewedByTrainerId(trainer.getId());
        latest.setReviewedByTrainerName(trainer.getFirstName() + " " + trainer.getLastName());
        latest.setReviewedAt(Instant.now());

        resume.setCurrentStatus(request.getStatus());
        resume.setUpdatedAt(Instant.now());
        resume = resumeRepository.save(resume);

        String title = request.getStatus() == ResumeStatus.APPROVED
                ? "Your resume has been approved"
                : "Your resume needs changes";
        String message = request.getStatus() == ResumeStatus.APPROVED
                ? "Great work — your resume has been reviewed and approved."
                : "Your resume has been reviewed. Please check the trainer's feedback and upload a revised version.";

        notificationService.sendToUser(studentId, title, message, "TRAINER");

        auditLogService.log(trainer.getId(), trainerEmail, "TRAINER", "RESUME_REVIEWED",
                "Reviewed resume v" + latest.getVersionNumber() + " for student " + studentId
                        + " -> " + request.getStatus(), null);

        return resume;
    }

    /**
     * "Analyze with AI" (draft mode) — extracts text from the resume PDF and
     * asks Gemini for structured feedback. Does NOT change status or notify
     * the student — purely a preview for the trainer. See
     * {@link #autoReviewStudentResume} for the version that actually
     * approves/rejects and notifies.
     */
    public ResumeAiAnalysis analyzeStudentResume(String trainerEmail, String studentId, boolean refresh) {
        User trainer = getTrainer(trainerEmail);
        verifyStudentAssignedToTrainer(trainer.getId(), studentId);

        Resume resume = resumeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("This student hasn't uploaded a resume yet"));

        if (resume.getVersions().isEmpty()) {
            throw new BadRequestException("This resume has no versions to analyze");
        }

        Resume.ResumeVersion latest = resume.getVersions().get(resume.getVersions().size() - 1);

        if (!refresh && latest.getAiAnalysis() != null) {
            return latest.getAiAnalysis();
        }

        ResumeAiAnalysis analysis = resumeAiService.analyze(extractText(latest));
        analysis.setAnalyzedAt(Instant.now());

        latest.setAiAnalysis(analysis);
        resume.setUpdatedAt(Instant.now());
        resumeRepository.save(resume);

        auditLogService.log(trainer.getId(), trainerEmail, "TRAINER", "RESUME_AI_ANALYZED",
                "AI-analyzed resume v" + latest.getVersionNumber() + " for student " + studentId, null);

        return analysis;
    }

    /**
     * "Auto-Review with AI" — analyzes the resume, decides APPROVED vs
     * NEEDS_CHANGES purely from the AI score against
     * {@code app.resume.ai.approval-threshold}, writes that decision onto
     * the resume exactly like a manual review would, and notifies the
     * student. No trainer confirmation step.
     */
    public AutoReviewResult autoReviewStudentResume(String trainerEmail, String studentId, boolean refresh) {
        User trainer = getTrainer(trainerEmail);
        verifyStudentAssignedToTrainer(trainer.getId(), studentId);

        Resume resume = resumeRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("This student hasn't uploaded a resume yet"));

        if (resume.getVersions().isEmpty()) {
            throw new BadRequestException("This resume has no versions to review");
        }

        return doAutoReview(trainer, resume, refresh);
    }

    /**
     * "Auto-Review All Pending" — runs {@link #doAutoReview} across every
     * PENDING_REVIEW resume in this trainer's batches in one call. One
     * failing resume (e.g. unreadable PDF) does not stop the rest — it's
     * reported as success=false in that student's result.
     */
    public List<AutoReviewResult> autoReviewAllPending(String trainerEmail, boolean refresh) {
        User trainer = getTrainer(trainerEmail);
        List<Resume> pending = listForReview(trainerEmail, ResumeStatus.PENDING_REVIEW);

        List<AutoReviewResult> results = new ArrayList<>();
        for (Resume resume : pending) {
            try {
                results.add(doAutoReview(trainer, resume, refresh));
            } catch (Exception e) {
                User student = userRepository.findById(resume.getStudentId()).orElse(null);
                String name = student != null ? student.getFirstName() + " " + student.getLastName() : null;
                results.add(AutoReviewResult.failed(resume.getStudentId(), name, e.getMessage()));
            }
        }
        return results;
    }

    /** Shared core: analyze (or reuse cache) -> decide status -> save -> notify -> audit log. */
    private AutoReviewResult doAutoReview(User trainer, Resume resume, boolean refresh) {
        Resume.ResumeVersion latest = resume.getVersions().get(resume.getVersions().size() - 1);

        ResumeAiAnalysis analysis;
        if (!refresh && latest.getAiAnalysis() != null) {
            analysis = latest.getAiAnalysis();
        } else {
            analysis = resumeAiService.analyze(extractText(latest));
            analysis.setAnalyzedAt(Instant.now());
            latest.setAiAnalysis(analysis);
        }

        ResumeStatus decidedStatus = (analysis.getOverallScore() != null && analysis.getOverallScore() >= aiApprovalThreshold)
                ? ResumeStatus.APPROVED
                : ResumeStatus.NEEDS_CHANGES;

        latest.setStatus(decidedStatus);
        latest.setScore(analysis.getOverallScore() != null ? analysis.getOverallScore().doubleValue() : null);
        latest.setSuggestions(analysis.getSuggestedReviewText());
        latest.setReviewedByTrainerId(trainer.getId());
        latest.setReviewedByTrainerName("AI Auto-Review");
        latest.setReviewedAt(Instant.now());

        resume.setCurrentStatus(decidedStatus);
        resume.setUpdatedAt(Instant.now());
        resumeRepository.save(resume);

        String title = decidedStatus == ResumeStatus.APPROVED
                ? "Your resume has been approved"
                : "Your resume needs changes";
        String message = decidedStatus == ResumeStatus.APPROVED
                ? "Great work — your resume has been reviewed and approved. You're ready for placement drives."
                : "Your resume has been reviewed. Please check the feedback and upload a revised version.";

        notificationService.sendToUser(resume.getStudentId(), title, message, "TRAINER");

        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", "RESUME_AI_AUTO_REVIEWED",
                "AI auto-reviewed resume v" + latest.getVersionNumber() + " for student " + resume.getStudentId()
                        + " -> " + decidedStatus + " (score " + analysis.getOverallScore() + ")", null);

        User student = userRepository.findById(resume.getStudentId()).orElse(null);
        String studentName = student != null ? student.getFirstName() + " " + student.getLastName() : null;

        return AutoReviewResult.builder()
                .studentId(resume.getStudentId())
                .studentName(studentName)
                .success(true)
                .status(decidedStatus)
                .score(analysis.getOverallScore())
                .analysis(analysis)
                .build();
    }

    private String extractText(Resume.ResumeVersion version) {
        Resource file = fileStorageService.loadAsResource(version.getFilePath());
        try (InputStream in = file.getInputStream()) {
            return pdfTextExtractionService.extractText(in, version.getFileName());
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the resume file for AI analysis");
        }
    }

    /** "Select Student" list view — every resume from students across this trainer's batches, optionally filtered by status. */
    public List<Resume> listForReview(String trainerEmail, ResumeStatus statusFilter) {
        User trainer = getTrainer(trainerEmail);
        List<Batch> myBatches = batchRepository.findByTechnicalTrainerIdOrSoftSkillTrainerId(trainer.getId(), trainer.getId());

        List<String> myStudentIds = myBatches.stream()
                .flatMap(b -> b.getStudentIds().stream())
                .distinct()
                .toList();

        if (myStudentIds.isEmpty()) {
            return List.of();
        }

        List<Resume> resumes = resumeRepository.findAll().stream()
                .filter(r -> myStudentIds.contains(r.getStudentId()))
                .toList();

        if (statusFilter != null) {
            return resumes.stream().filter(r -> r.getCurrentStatus() == statusFilter).toList();
        }
        return resumes;
    }

    // ==================== SHARED HELPERS ====================

    private void verifyStudentAssignedToTrainer(String trainerId, String studentId) {
        List<Batch> batchesWithStudent = batchRepository.findByStudentIdsContaining(studentId);
        boolean trainerTeachesOneOfThem = batchesWithStudent.stream()
                .anyMatch(b -> trainerId.equals(b.getTechnicalTrainerId()) || trainerId.equals(b.getSoftSkillTrainerId()));

        if (!trainerTeachesOneOfThem) {
            throw new BadRequestException("This student is not in any of your assigned batches");
        }
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer account not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only trainers can perform this action");
        }
        return user;
    }

    private User getStudent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student account not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can perform this action");
        }
        return user;
    }
}
