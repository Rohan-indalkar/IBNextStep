package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.quiz.dto.AutoSaveRequest;
import com.infobeans.ibnextstep.quiz.dto.StudentQuestionView;
import com.infobeans.ibnextstep.quiz.dto.SubmitQuizRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentQuizService {

    private final QuizRepository quizRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizResultRepository resultRepository;
    private final QuizAnswerRepository answerRepository;
    private final QuizViolationRepository violationRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final QuizEvaluationService evaluationService;
    private final NotificationService notificationService;

    private static final int MAX_WARNINGS_BEFORE_AUTO_SUBMIT = 3;

    public List<Quiz> listAssigned(String studentEmail) {
        User student = getStudent(studentEmail);
        List<Batch> myBatches = batchRepository.findByStudentIdsContaining(student.getId());
        List<String> batchIds = myBatches.stream().map(Batch::getId).toList();

        return batchIds.stream()
                .flatMap(id -> quizRepository.findByBatchIdAndStatus(id, QuizStatus.PUBLISHED).stream())
                .toList();
    }

    /** "Read Instructions" — metadata only, no questions yet (those only appear after Start, and even then with no answers). */
    public Quiz getInstructions(String studentEmail, String quizId) {
        User student = getStudent(studentEmail);
        Quiz quiz = getAssignedQuiz(student, quizId);
        return quiz;
    }

    /**
     * "Start Quiz" — creates the ONE allowed attempt. Every student gets a
     * different, independently-shuffled question set: this method never
     * reuses another student's ordering, and re-calling it for an existing
     * attempt just returns the SAME questions again (so refreshing the page
     * doesn't reshuffle mid-quiz).
     */
    public List<StudentQuestionView> start(String studentEmail, String quizId, String ipAddress, String userAgent) {
        User student = getStudent(studentEmail);
        Quiz quiz = getAssignedQuiz(student, quizId);

        Optional<QuizAttempt> existing = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId());
        if (existing.isPresent()) {
            if (existing.get().getStatus() != AttemptStatus.IN_PROGRESS) {
                throw new BadRequestException("You have already attempted this quiz — one attempt only");
            }
            return toStudentView(existing.get());
        }

        List<Quiz.QuizQuestionEntry> shuffledQuestions = new ArrayList<>(quiz.getQuestions());
        Collections.shuffle(shuffledQuestions);

        List<QuizAttempt.AssignedQuestion> assigned = new ArrayList<>();
        int order = 0;
        for (Quiz.QuizQuestionEntry entry : shuffledQuestions) {
            List<String> options = new ArrayList<>(entry.getQuestion().getOptions() == null ? List.of() : entry.getQuestion().getOptions());
            Collections.shuffle(options); // "Shuffle Options" — same question, different order per student

            assigned.add(QuizAttempt.AssignedQuestion.builder()
                    .assignmentId(UUID.randomUUID().toString())
                    .questionBankId(entry.getQuestionBankId())
                    .questionText(entry.getQuestion().getQuestionText())
                    .options(options)
                    .type(entry.getQuestion().getType())
                    .marks(entry.getQuestion().getMarks())
                    .order(order++)
                    .build());
        }

        QuizAttempt attempt = QuizAttempt.builder()
                .quizId(quizId)
                .studentId(student.getId())
                .assignedQuestions(assigned)
                .savedAnswers(new HashMap<>())
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .cheatingScore(0)
                .needsReview(false)
                .build();

        attemptRepository.save(attempt);
        return toStudentView(attempt);
    }

    /** "Auto Save Every 15 Seconds" — merges in whatever the frontend sends, doesn't require every answer every time. */
    public void autoSave(String studentEmail, String quizId, AutoSaveRequest request) {
        QuizAttempt attempt = getInProgressAttempt(studentEmail, quizId);
        attempt.getSavedAnswers().putAll(request.getAnswers());
        attempt.setLastAutoSaveAt(Instant.now());
        attemptRepository.save(attempt);
    }

    /** "Submit Quiz" -> "Auto Evaluation" -> "Result", all in one call. */
    public QuizResult submit(String studentEmail, String quizId, SubmitQuizRequest request) {
        QuizAttempt attempt = getInProgressAttempt(studentEmail, quizId);
        if (request != null && request.getAnswers() != null) {
            attempt.getSavedAnswers().putAll(request.getAnswers());
        }
        return finalizeAttempt(attempt, AttemptStatus.SUBMITTED);
    }

    /**
     * "Tab Switch / Fullscreen Detection" -> warning counters -> "Auto submit
     * after third warning." Every violation type is tracked independently.
     */
    public Map<String, Object> recordViolation(String studentEmail, String quizId, ViolationType type) {
        QuizAttempt attempt = getInProgressAttempt(studentEmail, quizId);

        int currentCount = switch (type) {
        case TAB_SWITCH -> {
            attempt.setTabSwitchCount(attempt.getTabSwitchCount() + 1);
            yield attempt.getTabSwitchCount();
        }
        case FULLSCREEN_EXIT -> {
            attempt.setFullscreenExitCount(attempt.getFullscreenExitCount() + 1);
            yield attempt.getFullscreenExitCount();
        }
        case COPY_ATTEMPT -> {
            attempt.setCopyAttemptCount(attempt.getCopyAttemptCount() + 1);
            yield attempt.getCopyAttemptCount();
        }
        case PASTE_ATTEMPT -> {
            attempt.setPasteAttemptCount(attempt.getPasteAttemptCount() + 1);
            yield attempt.getPasteAttemptCount();
        }
        case RIGHT_CLICK_ATTEMPT -> {
            attempt.setRightClickAttemptCount(attempt.getRightClickAttemptCount() + 1);
            yield attempt.getRightClickAttemptCount();
        }
        case FAST_SUBMISSION -> 1; // recorded at submit time, not incrementable here
    };
        attemptRepository.save(attempt);

        violationRepository.save(QuizViolation.builder()
                .attemptId(attempt.getId())
                .quizId(quizId)
                .studentId(attempt.getStudentId())
                .type(type)
                .warningNumber(currentCount)
                .occurredAt(Instant.now())
                .build());

        boolean autoSubmitted = false;
        if ((type == ViolationType.TAB_SWITCH || type == ViolationType.FULLSCREEN_EXIT)
                && currentCount >= MAX_WARNINGS_BEFORE_AUTO_SUBMIT) {
            finalizeAttempt(attempt, AttemptStatus.AUTO_SUBMITTED_VIOLATION);
            autoSubmitted = true;
        }

        return Map.of("warningNumber", currentCount, "autoSubmitted", autoSubmitted);
    }

    // ==================== internal ====================

    private QuizResult finalizeAttempt(QuizAttempt attempt, AttemptStatus finalStatus) {
        Quiz quiz = quizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + attempt.getQuizId()));

        int totalMarks = 0;
        double obtainedMarks = 0;
        int correct = 0, wrong = 0, pendingReview = 0;

        for (QuizAttempt.AssignedQuestion assigned : attempt.getAssignedQuestions()) {
            totalMarks += assigned.getMarks();
            QuestionBankItem bankItem = questionBankRepository.findById(assigned.getQuestionBankId())
                    .orElse(null);
            if (bankItem == null) continue;

            List<String> studentAnswer = attempt.getSavedAnswers().getOrDefault(assigned.getAssignmentId(), List.of());
            var evalResult = evaluationService.evaluate(assigned, bankItem, studentAnswer);

            answerRepository.save(QuizAnswer.builder()
                    .attemptId(attempt.getId())
                    .quizId(attempt.getQuizId())
                    .studentId(attempt.getStudentId())
                    .questionBankId(assigned.getQuestionBankId())
                    .questionText(assigned.getQuestionText())
                    .studentAnswer(studentAnswer)
                    .correct(evalResult.correct())
                    .marksAwarded(evalResult.marksAwarded())
                    .pendingManualReview(evalResult.pendingManualReview())
                    .build());

            obtainedMarks += evalResult.marksAwarded();
            if (evalResult.pendingManualReview()) pendingReview++;
            else if (evalResult.correct()) correct++;
            else wrong++;
        }

        double percentage = totalMarks == 0 ? 0 : (obtainedMarks * 100.0) / totalMarks;
        boolean passed = percentage >= quiz.getPassingPercentage();

        // Simple, explainable cheating heuristic — not ML, just thresholds. Real pattern-matching across students is out of scope for now.
        int cheatingScore = (attempt.getTabSwitchCount() * 10) + (attempt.getFullscreenExitCount() * 15)
                + (attempt.getCopyAttemptCount() * 5) + (attempt.getPasteAttemptCount() * 5)
                + (attempt.getRightClickAttemptCount() * 2);
        boolean needsReview = cheatingScore >= 30;

        attempt.setStatus(finalStatus);
        attempt.setSubmittedAt(Instant.now());
        attempt.setCheatingScore(cheatingScore);
        attempt.setNeedsReview(needsReview);
        attemptRepository.save(attempt);

        long durationTaken = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();

        QuizResult result = QuizResult.builder()
                .quizId(attempt.getQuizId())
                .studentId(attempt.getStudentId())
                .attemptId(attempt.getId())
                .totalMarks(totalMarks)
                .obtainedMarks(obtainedMarks)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .pendingManualGrading(pendingReview)
                .percentage(percentage)
                .passed(passed)
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .durationTakenSeconds(durationTaken)
                .cheatingScore(cheatingScore)
                .needsReview(needsReview)
                .build();

        result = resultRepository.save(result);

        notificationService.sendToUser(attempt.getStudentId(), "Quiz result: " + quiz.getTitle(),
                "You scored " + String.format("%.1f", percentage) + "% (" + (passed ? "Passed" : "Failed") + ").",
                "SYSTEM");

        return result;
    }

    private List<StudentQuestionView> toStudentView(QuizAttempt attempt) {
        return attempt.getAssignedQuestions().stream()
                .map(a -> StudentQuestionView.builder()
                        .assignmentId(a.getAssignmentId())
                        .questionText(a.getQuestionText())
                        .options(a.getOptions())
                        .type(a.getType())
                        .marks(a.getMarks())
                        .order(a.getOrder())
                        .build())
                .toList();
    }

    private QuizAttempt getInProgressAttempt(String studentEmail, String quizId) {
        User student = getStudent(studentEmail);
        QuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You haven't started this quiz yet"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("This attempt is already " + attempt.getStatus() + " — no further changes allowed");
        }
        return attempt;
    }

    private Quiz getAssignedQuiz(User student, String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        List<Batch> myBatches = batchRepository.findByStudentIdsContaining(student.getId());
        boolean assigned = myBatches.stream().anyMatch(b -> b.getId().equals(quiz.getBatchId()));
        if (!assigned) {
            throw new BadRequestException("This quiz is not assigned to you");
        }
        return quiz;
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
