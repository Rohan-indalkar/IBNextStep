package com.infobeans.ibnextstep.codingassessment;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.codingassessment.dto.*;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainerAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CodingQuestionRepository questionRepository;
    private final TestCaseRepository testCaseRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AiCodingQuestionService aiCodingQuestionService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // ==================== Assessment CRUD ====================

    public Assessment create(String trainerEmail, CreateAssessmentRequest request) {
        User trainer = getTrainer(trainerEmail);
        verifyBatchOwnership(trainer.getId(), request.getBatchId());

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        Assessment assessment = Assessment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .batchId(request.getBatchId())
                .durationMinutes(request.getDurationMinutes())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .passingMarks(request.getPassingMarks())
                .maxAttempts(request.getMaxAttempts())
                .allowedLanguages(request.getAllowedLanguages())
                .status(AssessmentStatus.DRAFT)
                .questionIds(new ArrayList<>())
                .trainerId(trainer.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assessment = assessmentRepository.save(assessment);
        audit(trainer, "ASSESSMENT_CREATED", "Created assessment '" + assessment.getTitle() + "'");
        return assessment;
    }

    public Assessment update(String trainerEmail, String assessmentId, UpdateAssessmentRequest request) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        if (request.getTitle() != null) assessment.setTitle(request.getTitle());
        if (request.getDescription() != null) assessment.setDescription(request.getDescription());
        if (request.getDurationMinutes() != null) assessment.setDurationMinutes(request.getDurationMinutes());
        if (request.getStartTime() != null) assessment.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) assessment.setEndTime(request.getEndTime());
        if (request.getPassingMarks() != null) assessment.setPassingMarks(request.getPassingMarks());
        if (request.getMaxAttempts() != null) assessment.setMaxAttempts(request.getMaxAttempts());
        assessment.setUpdatedAt(Instant.now());
        return assessmentRepository.save(assessment);
    }

    public void delete(String trainerEmail, String assessmentId) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        List<CodingQuestion> questions = questionRepository.findByAssessmentIdOrderByOrderAsc(assessmentId);
        for (CodingQuestion q : questions) {
            testCaseRepository.deleteAll(testCaseRepository.findByQuestionId(q.getId()));
        }
        questionRepository.deleteAll(questions);
        assessmentRepository.delete(assessment);
    }

    /** "Publish Assessment" -> notifies every student in the batch (email + WebSocket, via existing NotificationService). */
    public Assessment publish(String trainerEmail, String assessmentId) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        if (assessment.getQuestionIds().isEmpty()) {
            throw new BadRequestException("Cannot publish an assessment with no questions");
        }

        Batch batch = batchRepository.findById(assessment.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + assessment.getBatchId()));

        assessment.setStatus(AssessmentStatus.PUBLISHED);
        assessment.setPublishedAt(Instant.now());
        assessment.setUpdatedAt(Instant.now());
        Assessment published = assessmentRepository.save(assessment);

        for (String studentId : batch.getStudentIds()) {
            notificationService.sendToUser(studentId, "New Coding Assessment: " + published.getTitle(),
                    "A new coding assessment \"" + published.getTitle() + "\" has been assigned to you. "
                            + published.getQuestionIds().size() + " questions, " + published.getDurationMinutes() + " minutes.",
                    "TRAINER");
        }

        audit(getTrainer(trainerEmail), "ASSESSMENT_PUBLISHED",
                "Published assessment '" + published.getTitle() + "' to " + batch.getStudentIds().size() + " students");
        return published;
    }

    public Assessment archive(String trainerEmail, String assessmentId) {
        Assessment assessment = getOwnedAssessment(trainerEmail, assessmentId);
        assessment.setStatus(AssessmentStatus.ARCHIVED);
        assessment.setUpdatedAt(Instant.now());
        return assessmentRepository.save(assessment);
    }

    /** "Duplicate Assessment" — copies the assessment AND every question/test case, always landing back in DRAFT so it can be edited freely. */
    public Assessment duplicate(String trainerEmail, String assessmentId) {
        Assessment original = getOwnedAssessment(trainerEmail, assessmentId);

        Assessment copy = Assessment.builder()
                .title(original.getTitle() + " (Copy)")
                .description(original.getDescription())
                .batchId(original.getBatchId())
                .durationMinutes(original.getDurationMinutes())
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .passingMarks(original.getPassingMarks())
                .maxAttempts(original.getMaxAttempts())
                .allowedLanguages(original.getAllowedLanguages())
                .status(AssessmentStatus.DRAFT)
                .questionIds(new ArrayList<>())
                .trainerId(original.getTrainerId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        copy = assessmentRepository.save(copy);

        List<CodingQuestion> originalQuestions = questionRepository.findByAssessmentIdOrderByOrderAsc(assessmentId);
        for (CodingQuestion oq : originalQuestions) {
            CodingQuestion newQuestion = CodingQuestion.builder()
                    .assessmentId(copy.getId())
                    .title(oq.getTitle())
                    .problemStatement(oq.getProblemStatement())
                    .inputFormat(oq.getInputFormat())
                    .outputFormat(oq.getOutputFormat())
                    .constraints(oq.getConstraints())
                    .examples(oq.getExamples())
                    .difficulty(oq.getDifficulty())
                    .marks(oq.getMarks())
                    .timeLimitSeconds(oq.getTimeLimitSeconds())
                    .memoryLimitMb(oq.getMemoryLimitMb())
                    .allowedLanguages(oq.getAllowedLanguages())
                    .order(oq.getOrder())
                    .aiGenerated(oq.isAiGenerated())
                    .createdAt(Instant.now())
                    .build();
            newQuestion = questionRepository.save(newQuestion);
            copy.getQuestionIds().add(newQuestion.getId());

            for (TestCase otc : testCaseRepository.findByQuestionId(oq.getId())) {
                testCaseRepository.save(TestCase.builder()
                        .questionId(newQuestion.getId())
                        .input(otc.getInput())
                        .expectedOutput(otc.getExpectedOutput())
                        .hidden(otc.isHidden())
                        .build());
            }
        }
        return assessmentRepository.save(copy);
    }

    public Page<Assessment> list(String trainerEmail, Pageable pageable) {
        return assessmentRepository.findByTrainerId(getTrainer(trainerEmail).getId(), pageable);
    }

    public Assessment getOne(String trainerEmail, String assessmentId) {
        return getOwnedAssessment(trainerEmail, assessmentId);
    }

    /**
     * "Status: Completed" — once a PUBLISHED assessment's end time passes,
     * it's no longer actively open for students, so it moves to COMPLETED
     * automatically and the trainer gets the "Assessment Completed"
     * notification your spec calls for. Runs every minute.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60_000)
    public void completeExpiredAssessments() {
        List<Assessment> due = assessmentRepository.findByStatusAndEndTimeBefore(AssessmentStatus.PUBLISHED, Instant.now());
        for (Assessment assessment : due) {
            assessment.setStatus(AssessmentStatus.COMPLETED);
            assessment.setUpdatedAt(Instant.now());
            assessmentRepository.save(assessment);

            notificationService.sendToUser(assessment.getTrainerId(), "Assessment completed: " + assessment.getTitle(),
                    "\"" + assessment.getTitle() + "\" has ended and moved to Completed status.", "SYSTEM");
        }
    }

    // ==================== Questions ====================

    public CodingQuestion addQuestion(String trainerEmail, String assessmentId, CreateQuestionRequest request) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        CodingQuestion question = saveQuestionWithTestCases(assessment, request, false);
        assessment.getQuestionIds().add(question.getId());
        assessment.setUpdatedAt(Instant.now());
        assessmentRepository.save(assessment);
        return question;
    }

    /** "Create Assessment using AI" step 2 — trainer already got AI suggestions via generateQuestions(), now saves the ones they kept/edited. */
    public List<CodingQuestion> addAiGeneratedQuestions(String trainerEmail, String assessmentId, List<CreateQuestionRequest> requests) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        List<CodingQuestion> saved = new ArrayList<>();
        for (CreateQuestionRequest r : requests) {
            CodingQuestion question = saveQuestionWithTestCases(assessment, r, true);
            assessment.getQuestionIds().add(question.getId());
            saved.add(question);
        }
        assessment.setUpdatedAt(Instant.now());
        assessmentRepository.save(assessment);
        return saved;
    }

    /** AI GENERATION step 1 — trainer previews before anything is saved. */
    public List<CreateQuestionRequest> generateQuestionsPreview(String trainerEmail, GenerateQuestionsRequest request) {
        getTrainer(trainerEmail);
        return aiCodingQuestionService.generate(request);
    }

    public CodingQuestion editQuestion(String trainerEmail, String assessmentId, String questionId, CreateQuestionRequest request) {
        getOwnedDraftAssessment(trainerEmail, assessmentId);
        CodingQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (!question.getAssessmentId().equals(assessmentId)) {
            throw new BadRequestException("That question does not belong to this assessment");
        }

        testCaseRepository.deleteAll(testCaseRepository.findByQuestionId(questionId));

        question.setTitle(request.getTitle());
        question.setProblemStatement(request.getProblemStatement());
        question.setInputFormat(request.getInputFormat());
        question.setOutputFormat(request.getOutputFormat());
        question.setConstraints(request.getConstraints());
        question.setExamples(request.getExamples());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks());
        question.setTimeLimitSeconds(request.getTimeLimitSeconds());
        question.setMemoryLimitMb(request.getMemoryLimitMb());
        question.setAllowedLanguages(request.getAllowedLanguages());
        question = questionRepository.save(question);

        saveTestCases(question.getId(), request);
        return question;
    }

    public void deleteQuestion(String trainerEmail, String assessmentId, String questionId) {
        Assessment assessment = getOwnedDraftAssessment(trainerEmail, assessmentId);
        testCaseRepository.deleteAll(testCaseRepository.findByQuestionId(questionId));
        questionRepository.deleteById(questionId);
        assessment.getQuestionIds().remove(questionId);
        assessment.setUpdatedAt(Instant.now());
        assessmentRepository.save(assessment);
    }

    /** "Regenerate" one question via AI, keeping its position in the assessment. */
    public CodingQuestion regenerateQuestion(String trainerEmail, String assessmentId, String questionId, GenerateQuestionsRequest regenRequest) {
        getOwnedDraftAssessment(trainerEmail, assessmentId);
        CodingQuestion existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        GenerateQuestionsRequest req = new GenerateQuestionsRequest();
        req.setTopic(regenRequest.getTopic() != null ? regenRequest.getTopic() : existing.getTitle());
        req.setLanguage(regenRequest.getLanguage());
        req.setDifficulty(regenRequest.getDifficulty() != null ? regenRequest.getDifficulty() : existing.getDifficulty());
        req.setQuestionCount(1);

        List<CreateQuestionRequest> generated = aiCodingQuestionService.generate(req);
        CreateQuestionRequest replacement = generated.get(0);

        testCaseRepository.deleteAll(testCaseRepository.findByQuestionId(questionId));

        existing.setTitle(replacement.getTitle());
        existing.setProblemStatement(replacement.getProblemStatement());
        existing.setInputFormat(replacement.getInputFormat());
        existing.setOutputFormat(replacement.getOutputFormat());
        existing.setConstraints(replacement.getConstraints());
        existing.setExamples(replacement.getExamples());
        existing.setDifficulty(replacement.getDifficulty());
        existing.setMarks(replacement.getMarks());
        existing.setTimeLimitSeconds(replacement.getTimeLimitSeconds());
        existing.setMemoryLimitMb(replacement.getMemoryLimitMb());
        existing.setAllowedLanguages(replacement.getAllowedLanguages());
        existing = questionRepository.save(existing);

        saveTestCases(existing.getId(), replacement);
        return existing;
    }

    public List<CodingQuestion> listQuestions(String trainerEmail, String assessmentId) {
        getOwnedAssessment(trainerEmail, assessmentId);
        return questionRepository.findByAssessmentIdOrderByOrderAsc(assessmentId);
    }

    public List<TestCase> listAllTestCases(String trainerEmail, String questionId) {
        // Trainer sees hidden test cases too — only Student APIs hide them.
        return testCaseRepository.findByQuestionId(questionId);
    }

    // ==================== helpers ====================

    private CodingQuestion saveQuestionWithTestCases(Assessment assessment, CreateQuestionRequest request, boolean aiGenerated) {
        int nextOrder = questionRepository.findByAssessmentIdOrderByOrderAsc(assessment.getId()).size();

        CodingQuestion question = CodingQuestion.builder()
                .assessmentId(assessment.getId())
                .title(request.getTitle())
                .problemStatement(request.getProblemStatement())
                .inputFormat(request.getInputFormat())
                .outputFormat(request.getOutputFormat())
                .constraints(request.getConstraints())
                .examples(request.getExamples())
                .difficulty(request.getDifficulty())
                .marks(request.getMarks())
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .memoryLimitMb(request.getMemoryLimitMb())
                .allowedLanguages(request.getAllowedLanguages())
                .order(nextOrder)
                .aiGenerated(aiGenerated)
                .createdAt(Instant.now())
                .build();
        question = questionRepository.save(question);
        saveTestCases(question.getId(), request);
        return question;
    }

    private void saveTestCases(String questionId, CreateQuestionRequest request) {
        for (CreateQuestionRequest.TestCaseInput tc : request.getPublicTestCases()) {
            testCaseRepository.save(TestCase.builder()
                    .questionId(questionId).input(tc.getInput()).expectedOutput(tc.getExpectedOutput()).hidden(false).build());
        }
        if (request.getHiddenTestCases() != null) {
            for (CreateQuestionRequest.TestCaseInput tc : request.getHiddenTestCases()) {
                testCaseRepository.save(TestCase.builder()
                        .questionId(questionId).input(tc.getInput()).expectedOutput(tc.getExpectedOutput()).hidden(true).build());
            }
        }
    }

    private Assessment getOwnedDraftAssessment(String trainerEmail, String assessmentId) {
        Assessment assessment = getOwnedAssessment(trainerEmail, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new BadRequestException("Only a DRAFT assessment can be edited — this one is " + assessment.getStatus());
        }
        return assessment;
    }

    private Assessment getOwnedAssessment(String trainerEmail, String assessmentId) {
        User trainer = getTrainer(trainerEmail);
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        if (!assessment.getTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only manage assessments you created");
        }
        return assessment;
    }

    private void verifyBatchOwnership(String trainerId, String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        boolean isMine = trainerId.equals(batch.getTechnicalTrainerId()) || trainerId.equals(batch.getSoftSkillTrainerId());
        if (!isMine) {
            throw new BadRequestException("You are not assigned to batch: " + batch.getName());
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

    private void audit(User trainer, String action, String details) {
        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", action, details, null);
    }
}
