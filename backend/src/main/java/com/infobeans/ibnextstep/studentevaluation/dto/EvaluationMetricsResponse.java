package com.infobeans.ibnextstep.studentevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * What the trainer sees BEFORE scoring — the system's auto-pulled numbers
 * and its own eligibility verdict, so the trainer has full context before
 * adding their own rubric scores and remarks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationMetricsResponse {
    private String studentId;
    private String studentName;

    private Double attendancePercentage;
    private Double avgQuizPercentage;
    private Double avgCodingPercentage;
    private Double avgMockInterviewRating;

    private boolean systemEligible;
    @Builder.Default
    private List<String> systemIneligibilityReasons = List.of();

    // ---------- Thresholds used for this computation, shown for transparency ----------
    private double minAttendancePercentage;
    private double minQuizPercentage;
    private double minCodingPercentage;
    private double minMockInterviewRating;
}
