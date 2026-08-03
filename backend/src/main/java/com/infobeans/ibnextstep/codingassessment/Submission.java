package com.infobeans.ibnextstep.codingassessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "submission")
public class Submission {

    @Id
    private String id;

    private String sessionId;
    private String assessmentId;
    private String questionId;
    private String studentId;

    private ProgrammingLanguage language;
    private String code;

    /** true for "Run" (public test cases only, no marks) — false for "Submit" (public + hidden, marks calculated). */
    private boolean runOnly;

    private SubmissionStatus status;
    private double marksAwarded;

    private Long executionTimeMs;
    private Long memoryUsedKb;
    private String compilationOutput;

    private List<TestCaseResult> testCaseResults;

    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private String testCaseId;
        private boolean hidden;
        private boolean passed;
        /** Only populated for public test cases — never leak a hidden test case's actual/expected output to a student. */
        private String actualOutput;
        private String expectedOutput;
    }
}
