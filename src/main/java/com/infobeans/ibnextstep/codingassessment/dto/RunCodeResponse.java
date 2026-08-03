package com.infobeans.ibnextstep.codingassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** "Do NOT calculate marks" — this response deliberately has no marks/status field, just raw execution feedback. */
@Data
@Builder
@AllArgsConstructor
public class RunCodeResponse {
    private boolean compiled;
    private String compilationOutput;
    private Long executionTimeMs;
    private List<PublicCaseOutcome> results;

    @Data
    @Builder
    @AllArgsConstructor
    public static class PublicCaseOutcome {
        private String testCaseId;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private boolean passed;
    }
}
