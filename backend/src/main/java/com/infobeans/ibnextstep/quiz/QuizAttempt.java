package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One document per student per quiz — "One Attempt Only" is enforced by
 * simply refusing to create a second one for the same (quizId, studentId).
 * assignedQuestions is THIS student's own randomized subset with THIS
 * student's own shuffled option order — never regenerated after start,
 * so their quiz stays consistent even if they refresh the page.
 */
@Data
@Builder

@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_attempt")
public class QuizAttempt {

    @Id
    private String id;

    private String quizId;
    private String studentId;

    private List<AssignedQuestion> assignedQuestions;

    /** questionBankId (or synthetic id) -> the student's currently saved answer(s), updated by autosave. */
    private Map<String, List<String>> savedAnswers;

    private AttemptStatus status;

    private Instant startedAt;
    private Instant submittedAt;
    private Instant lastAutoSaveAt;

    private String ipAddress;
    private String userAgent;

    private int tabSwitchCount;
    private int fullscreenExitCount;
    private int copyAttemptCount;
    private int pasteAttemptCount;
    private int rightClickAttemptCount;

    /** Simple, explainable heuristic score (not ML) — see QuizAnalyticsService. Trainer dashboard shows this as "Cheating Score". */
    private int cheatingScore;
    private boolean needsReview;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedQuestion {
        /** Stable ID for this question WITHIN this attempt — used as the key in savedAnswers, since two different students may see the same bank question in different positions. */
        private String assignmentId;
        private String questionBankId;
        private String questionText;
        /** Already shuffled, unique to this student. */
        private List<String> options;
        private QuestionType type;
        private int marks;
        private int order;
    }
}
