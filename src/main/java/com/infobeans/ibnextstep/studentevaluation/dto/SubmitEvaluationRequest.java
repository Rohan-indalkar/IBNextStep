package com.infobeans.ibnextstep.studentevaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitEvaluationRequest {

    /** Skill name -> score out of 10. Must match the rubric for the trainer's own type. */
    @NotEmpty(message = "At least one rubric score is required")
    private Map<String, Integer> skillScores;

    @NotNull(message = "Remarks are required")
    private String remarks;

    /**
     * Trainer's final eligibility call. If omitted, the system's own
     * computed eligibility is used as-is. If provided and it disagrees
     * with the system's verdict, overrideReason becomes required.
     */
    private Boolean finalEligibleOverride;

    /** Required only when finalEligibleOverride disagrees with the system's verdict. */
    private String overrideReason;
}
