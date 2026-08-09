package com.infobeans.ibnextstep.codingassessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "assessmentsession")
public class AssessmentSession {

    @Id
    private String id;

    private String assessmentId;
    private String studentId;
    private int attemptNumber;

    private SessionStatus status;
    private int currentQuestionIndex;

    private Instant startedAt;
    private Instant submittedAt;

    private String ipAddress;
    private String browser;
    private String operatingSystem;

    private int tabSwitchCount;
    private int windowMinimizedCount;
    private int fullscreenExitCount;
    private int copyAttemptCount;
    private int pasteAttemptCount;
    private int devToolsOpenedCount;

    private double totalMarksAwarded;
    private int cheatingScore;
    private boolean needsReview;

    /** questionId -> the student's in-progress code for that question, saved via "Save Draft" without being compiled/graded. */
    private java.util.Map<String, DraftCode> drafts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DraftCode {
        private ProgrammingLanguage language;
        private String code;
        private Instant savedAt;
    }
}
