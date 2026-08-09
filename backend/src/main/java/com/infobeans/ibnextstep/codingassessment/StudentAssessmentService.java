package com.infobeans.ibnextstep.codingassessment;

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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CodingQuestionRepository questionRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final WarningLogRepository warningLogRepository;
    private final ActivityLogRepository activityLogRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final CodeExecutionService codeExecutionService;
    private final NotificationService notificationService;

    private static final int MAX_WARNINGS_BEFORE_AUTO_SUBMIT = 3;

    public List<Assessment> listAssigned(String studentEmail) {
        User student = getStudent(studentEmail);
        List<Batch> myBatches = batchRepository.findByStudentIdsContaining(student.getId());
        return myBatches.stream()
                .flatMap(b -> assessmentRepository.findByBatchIdAndStatus(b.getId(), AssessmentStatus.PUBLISHED).stream())
                .toList();
    }

    /** "Student Starts Assessment" -> creates the AssessmentSession, respecting maxAttempts. */
    public AssessmentSession start(String studentEmail, String assessmentId, String ipAddress, String browser, String os) {
        User student = getStudent(studentEmail);
        Assessment assessment = getAssignedAssessment(student, assessmentId);

        Optional<AssessmentSession> latest = sessionRepository.findTopByAssessmentIdAndStudentIdOrderByAttemptNumberDesc(assessmentId, student.getId());

        if (latest.isPresent() && latest.get().getStatus() == SessionStatus.IN_PROGRESS) {
            return latest.get(); // resume, don't create a duplicate session
        }

        int attemptsUsed = (int) sessionRepository.findByAssessmentIdAndStudentId(assessmentId, student.getId()).stream()
                .filter(s -> s.getStatus() != SessionStatus.IN_PROGRESS)
                .count();
        if (attemptsUsed >= assessment.getMaxAttempts()) {
            throw new BadRequestException("You have used all " + assessment.getMaxAttempts() + " allowed attempt(s) for this assessment");
        }

        AssessmentSession session = AssessmentSession.builder()
                .assessmentId(assessmentId)
                .studentId(student.getId())
                .attemptNumber(attemptsUsed + 1)
                .status(SessionStatus.IN_PROGRESS)
                .currentQuestionIndex(0)
                .startedAt(Instant.now())
                .ipAddress(ipAddress)
                .browser(browser)
                .operatingSystem(os)
                .drafts(new HashMap<>())
                .cheatingScore(0)
                .needsReview(false)
                .build();
        session = sessionRepository.save(session);

        logActivity(session, ActivityAction.ASSESSMENT_STARTED, "Assessment started, attempt #" + session.getAttemptNumber());
        notifyTrainer(assessment, "Student started assessment",
                student.getFirstName() + " " + student.getLastName() + " started \"" + assessment.getTitle() + "\".");

        return session;
    }

    public QuestionStudentView getQuestion(String studentEmail, String assessmentId, String questionId) {
        User student = getStudent(studentEmail);
        getAssignedAssessment(student, assessmentId);
        CodingQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (!question.getAssessmentId().equals(assessmentId)) {
            throw new BadRequestException("That question does not belong to this assessment");
        }

        List<TestCase> publicCases = testCaseRepository.findByQuestionIdAndHidden(questionId, false);

        return QuestionStudentView.builder()
                .id(question.getId())
                .title(question.getTitle())
                .problemStatement(question.getProblemStatement())
                .inputFormat(question.getInputFormat())
                .outputFormat(question.getOutputFormat())
                .constraints(question.getConstraints())
                .examples(question.getExamples())
                .difficulty(question.getDifficulty())
                .marks(question.getMarks())
                .timeLimitSeconds(question.getTimeLimitSeconds())
                .memoryLimitMb(question.getMemoryLimitMb())
                .allowedLanguages(question.getAllowedLanguages())
                .publicTestCases(publicCases.stream()
                        .map(tc -> QuestionStudentView.PublicTestCaseView.builder()
                                .id(tc.getId()).input(tc.getInput()).expectedOutput(tc.getExpectedOutput()).build())
                        .toList())
                .order(question.getOrder())
                .build();
    }

    /** "Previous Question" / "Next Question" — just moves the session's cursor, logged for the activity trail. */
    public AssessmentSession navigate(String studentEmail, String assessmentId, NavigateRequest request) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);
        session.setCurrentQuestionIndex(request.getQuestionIndex());
        session = sessionRepository.save(session);
        logActivity(session, ActivityAction.QUESTION_CHANGED, "Moved to question index " + request.getQuestionIndex());
        return session;
    }

    /** "Save Draft" — stores in-progress code without compiling or grading it. */
    public void saveDraft(String studentEmail, String assessmentId, String questionId, SaveDraftRequest request) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);
        session.getDrafts().put(questionId, AssessmentSession.DraftCode.builder()
                .language(request.getLanguage())
                .code(request.getCode())
                .savedAt(Instant.now())
                .build());
        sessionRepository.save(session);
    }

    /** "Run Code" — public test cases only, no marks calculated, matches spec exactly. */
    public RunCodeResponse runCode(String studentEmail, String assessmentId, String questionId, RunCodeRequest request) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);
        CodingQuestion question = getQuestionOrThrow(assessmentId, questionId);
        verifyLanguageAllowed(question, request.getLanguage());

        List<TestCase> publicCases = testCaseRepository.findByQuestionIdAndHidden(questionId, false);
        var executions = codeExecutionService.runAgainstCases(request.getLanguage(), request.getCode(), publicCases,
                question.getTimeLimitSeconds(), question.getMemoryLimitMb());

        submissionRepository.save(buildSubmissionRecord(session, question, request.getLanguage(), request.getCode(), true, executions));
        logActivity(session, ActivityAction.RUN_CODE, "Ran code for question " + question.getTitle());

        boolean anyCompilationError = executions.stream().anyMatch(e -> e.status() == SubmissionStatus.COMPILATION_ERROR);
        String compileOutput = executions.stream().map(CodeExecutionService.CaseExecution::compileOutput)
                .filter(Objects::nonNull).findFirst().orElse(null);
        Long execTime = executions.stream().map(CodeExecutionService.CaseExecution::executionTimeMs)
                .filter(Objects::nonNull).max(Long::compareTo).orElse(null);

        return RunCodeResponse.builder()
                .compiled(!anyCompilationError)
                .compilationOutput(compileOutput)
                .executionTimeMs(execTime)
                .results(executions.stream().map(e -> RunCodeResponse.PublicCaseOutcome.builder()
                        .testCaseId(e.testCase().getId())
                        .input(e.testCase().getInput())
                        .expectedOutput(e.testCase().getExpectedOutput())
                        .actualOutput(e.actualOutput())
                        .passed(e.passed())
                        .build()).toList())
                .build();
    }

    /** "Submit" — compiles, runs public + hidden test cases, calculates marks, stores everything. Trainer never manually grades this. */
    public Submission submitQuestion(String studentEmail, String assessmentId, String questionId, SubmitCodeRequest request) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);
        CodingQuestion question = getQuestionOrThrow(assessmentId, questionId);
        verifyLanguageAllowed(question, request.getLanguage());

        List<TestCase> allCases = testCaseRepository.findByQuestionId(questionId);
        var executions = codeExecutionService.runAgainstCases(request.getLanguage(), request.getCode(), allCases,
                question.getTimeLimitSeconds(), question.getMemoryLimitMb());

        Submission submission = buildSubmissionRecord(session, question, request.getLanguage(), request.getCode(), false, executions);
        submission = submissionRepository.save(submission);

        session.setTotalMarksAwarded(session.getTotalMarksAwarded() + submission.getMarksAwarded());
        sessionRepository.save(session);

        logActivity(session, ActivityAction.SUBMIT, "Submitted question " + question.getTitle()
                + " -> " + submission.getStatus() + " (" + submission.getMarksAwarded() + " marks)");

        return submission;
    }

    /** "Review" — everything submitted so far in this session, across all questions. */
    public List<Submission> reviewSubmissions(String studentEmail, String assessmentId) {
        AssessmentSession session = getSessionAnyStatus(studentEmail, assessmentId);
        return submissionRepository.findByAssessmentIdAndStudentIdAndRunOnlyFalse(assessmentId, session.getStudentId());
    }

    /** Final "Submit" of the whole assessment (distinct from per-question submit) — locks the session. */
    public AssessmentSession completeAssessment(String studentEmail, String assessmentId) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);
        return finalizeSession(session, SessionStatus.SUBMITTED);
    }

    /** Anti-cheating: every event increases a warning count; 3rd warning of tab-switch/fullscreen/dev-tools auto-submits. */
    public Map<String, Object> recordWarning(String studentEmail, String assessmentId, WarningType type) {
        AssessmentSession session = getInProgressSession(studentEmail, assessmentId);

        int currentCount = switch (type) {
            case TAB_SWITCH -> { session.setTabSwitchCount(session.getTabSwitchCount() + 1); yield session.getTabSwitchCount(); }
            case WINDOW_MINIMIZED -> { session.setWindowMinimizedCount(session.getWindowMinimizedCount() + 1); yield session.getWindowMinimizedCount(); }
            case FULLSCREEN_EXIT -> { session.setFullscreenExitCount(session.getFullscreenExitCount() + 1); yield session.getFullscreenExitCount(); }
            case COPY_ATTEMPT -> { session.setCopyAttemptCount(session.getCopyAttemptCount() + 1); yield session.getCopyAttemptCount(); }
            case PASTE_ATTEMPT -> { session.setPasteAttemptCount(session.getPasteAttemptCount() + 1); yield session.getPasteAttemptCount(); }
            case DEV_TOOLS_OPENED -> { session.setDevToolsOpenedCount(session.getDevToolsOpenedCount() + 1); yield session.getDevToolsOpenedCount(); }
        };
        sessionRepository.save(session);

        WarningLog log = WarningLog.builder()
                .sessionId(session.getId()).assessmentId(assessmentId).studentId(session.getStudentId())
                .type(type).warningNumber(currentCount).occurredAt(Instant.now()).build();
        warningLogRepository.save(log);
        logActivity(session, ActivityAction.WARNING, type + " (warning #" + currentCount + ")");

        boolean autoSubmitted = false;
        if (currentCount >= MAX_WARNINGS_BEFORE_AUTO_SUBMIT) {
            finalizeSession(session, SessionStatus.AUTO_SUBMITTED_VIOLATION);
            logActivity(session, ActivityAction.AUTO_SUBMIT, "Auto-submitted after 3rd " + type + " warning");
            autoSubmitted = true;
        }

        return Map.of("warningNumber", currentCount, "autoSubmitted", autoSubmitted);
    }

    /** Safety net — students who close the tab and never call anything again. Runs every minute. */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60_000)
    public void autoSubmitExpiredSessions() {
        for (AssessmentSession session : sessionRepository.findByStatus(SessionStatus.IN_PROGRESS)) {
            Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
            if (assessment == null) continue;
            Instant deadline = session.getStartedAt().plus(Duration.ofMinutes(assessment.getDurationMinutes()));
            if (Instant.now().isAfter(deadline) || Instant.now().isAfter(assessment.getEndTime())) {
                finalizeSession(session, SessionStatus.AUTO_SUBMITTED_TIMER);
                logActivity(session, ActivityAction.AUTO_SUBMIT, "Auto-submitted — time expired");
            }
        }
    }

    // ==================== internal ====================

    private AssessmentSession finalizeSession(AssessmentSession session, SessionStatus status) {
        session.setStatus(status);
        session.setSubmittedAt(Instant.now());

        int cheatingScore = (session.getTabSwitchCount() * 10) + (session.getWindowMinimizedCount() * 8)
                + (session.getFullscreenExitCount() * 15) + (session.getCopyAttemptCount() * 5)
                + (session.getPasteAttemptCount() * 5) + (session.getDevToolsOpenedCount() * 20);
        session.setCheatingScore(cheatingScore);
        session.setNeedsReview(cheatingScore >= 30);

        session = sessionRepository.save(session);
        logActivity(session, ActivityAction.ASSESSMENT_COMPLETED, "Assessment completed with status " + status);

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
        if (assessment != null) {
            notificationService.sendToUser(session.getStudentId(), "Assessment submitted: " + assessment.getTitle(),
                    "Your submission for \"" + assessment.getTitle() + "\" has been recorded. Total marks: " + session.getTotalMarksAwarded() + ".",
                    "SYSTEM");
            User student = userRepository.findById(session.getStudentId()).orElse(null);
            notifyTrainer(assessment, "Student submitted assessment",
                    (student != null ? student.getFirstName() + " " + student.getLastName() : "A student")
                            + " submitted \"" + assessment.getTitle() + "\" — " + session.getTotalMarksAwarded() + " marks.");
        }
        return session;
    }

    private Submission buildSubmissionRecord(AssessmentSession session, CodingQuestion question, ProgrammingLanguage language,
                                              String code, boolean runOnly, List<CodeExecutionService.CaseExecution> executions) {
        boolean allPassed = executions.stream().allMatch(CodeExecutionService.CaseExecution::passed);
        SubmissionStatus overallStatus = executions.stream()
                .map(CodeExecutionService.CaseExecution::status)
                .filter(s -> s != SubmissionStatus.ACCEPTED)
                .findFirst()
                .orElse(allPassed ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER);

        double marksAwarded = 0;
        if (!runOnly && overallStatus == SubmissionStatus.ACCEPTED) {
            marksAwarded = question.getMarks();
        }

        Long maxTime = executions.stream().map(CodeExecutionService.CaseExecution::executionTimeMs).filter(Objects::nonNull).max(Long::compareTo).orElse(null);
        Long maxMemory = executions.stream().map(CodeExecutionService.CaseExecution::memoryKb).filter(Objects::nonNull).max(Long::compareTo).orElse(null);
        String compileOutput = executions.stream().map(CodeExecutionService.CaseExecution::compileOutput).filter(Objects::nonNull).findFirst().orElse(null);

        List<Submission.TestCaseResult> results = executions.stream()
                .map(e -> Submission.TestCaseResult.builder()
                        .testCaseId(e.testCase().getId())
                        .hidden(e.testCase().isHidden())
                        .passed(e.passed())
                        .actualOutput(e.testCase().isHidden() ? null : e.actualOutput())
                        .expectedOutput(e.testCase().isHidden() ? null : e.testCase().getExpectedOutput())
                        .build())
                .toList();

        return Submission.builder()
                .sessionId(session.getId())
                .assessmentId(session.getAssessmentId())
                .questionId(question.getId())
                .studentId(session.getStudentId())
                .language(language)
                .code(code)
                .runOnly(runOnly)
                .status(overallStatus)
                .marksAwarded(marksAwarded)
                .executionTimeMs(maxTime)
                .memoryUsedKb(maxMemory)
                .compilationOutput(compileOutput)
                .testCaseResults(results)
                .createdAt(Instant.now())
                .build();
    }

    private void logActivity(AssessmentSession session, ActivityAction action, String details) {
        activityLogRepository.save(ActivityLog.builder()
                .sessionId(session.getId())
                .assessmentId(session.getAssessmentId())
                .studentId(session.getStudentId())
                .action(action)
                .details(details)
                .timestamp(Instant.now())
                .ipAddress(session.getIpAddress())
                .browser(session.getBrowser())
                .operatingSystem(session.getOperatingSystem())
                .build());
    }

    private void notifyTrainer(Assessment assessment, String title, String message) {
        notificationService.sendToUser(assessment.getTrainerId(), title, message, "SYSTEM");
    }

    private void verifyLanguageAllowed(CodingQuestion question, ProgrammingLanguage language) {
        if (!question.getAllowedLanguages().contains(language)) {
            throw new BadRequestException("This question does not allow " + language);
        }
    }

    private CodingQuestion getQuestionOrThrow(String assessmentId, String questionId) {
        CodingQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (!question.getAssessmentId().equals(assessmentId)) {
            throw new BadRequestException("That question does not belong to this assessment");
        }
        return question;
    }

    private AssessmentSession getInProgressSession(String studentEmail, String assessmentId) {
        AssessmentSession session = getSessionAnyStatus(studentEmail, assessmentId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BadRequestException("This session is already " + session.getStatus() + " — no further changes allowed");
        }
        return session;
    }

    private AssessmentSession getSessionAnyStatus(String studentEmail, String assessmentId) {
        User student = getStudent(studentEmail);
        return sessionRepository.findTopByAssessmentIdAndStudentIdOrderByAttemptNumberDesc(assessmentId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You haven't started this assessment yet"));
    }

    private Assessment getAssignedAssessment(User student, String assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        List<Batch> myBatches = batchRepository.findByStudentIdsContaining(student.getId());
        boolean assigned = myBatches.stream().anyMatch(b -> b.getId().equals(assessment.getBatchId()));
        if (!assigned) {
            throw new BadRequestException("This assessment is not assigned to you");
        }
        return assessment;
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
