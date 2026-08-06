package com.infobeans.ibnextstep.placement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckResponse {

    private boolean eligible;

    /** Every rule the student fails — never short-circuited to the first failure. Empty when eligible. */
    @Builder.Default
    private List<FailedCriterion> failedCriteria = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedCriterion {
        /** e.g. "Attendance", "Coding Assessment Score", "Quiz Score", "Mock Interview", "Student Evaluation", "Resume". */
        private String criterion;
        private String required;
        private String current;
        private String reason;
    }
}
