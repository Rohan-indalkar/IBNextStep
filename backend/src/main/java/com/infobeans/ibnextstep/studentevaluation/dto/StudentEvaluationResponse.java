package com.infobeans.ibnextstep.studentevaluation.dto;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEvaluationResponse {
    private String id;
    private String studentId;
    private String studentName;
    private String batchId;

    private String trainerId;
    private String trainerName;
    private TrainerType trainerType;

    private Double attendancePercentage;
    private Double avgQuizPercentage;
    private Double avgCodingPercentage;
    private Double avgMockInterviewRating;

    private boolean systemEligible;
    @Builder.Default
    private List<String> systemIneligibilityReasons = List.of();

    private Map<String, Integer> skillScores;
    private Double overallRubricScore;

    private String remarks;

    private boolean finalEligible;
    private String overrideReason;

    private Instant evaluatedAt;

    private boolean edited;
    private Instant updatedAt;
    private String lastEditedBy;
}
