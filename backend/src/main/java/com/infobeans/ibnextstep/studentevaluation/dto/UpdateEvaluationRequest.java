package com.infobeans.ibnextstep.studentevaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Correcting a mistake on an already-submitted evaluation. Same shape as
 * SubmitEvaluationRequest — the trainer resupplies the full rubric scoring
 * and remarks, which replace what was there before. The underlying
 * system-metrics snapshot (attendance/quiz/coding/mock-interview) is
 * recomputed fresh at edit time so the corrected record reflects the
 * student's latest standing, not stale numbers from the original submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEvaluationRequest {

    /** Skill name -> score out of 10. Must match the rubric for the trainer's own type. */
    @NotEmpty(message = "At least one rubric score is required")
    private Map<String, Integer> skillScores;

    @NotNull(message = "Remarks are required")
    private String remarks;

    /**
     * Trainer's final eligibility call. If omitted, the system's own
     * (freshly recomputed) eligibility is used as-is. If provided and it
     * disagrees with the system's verdict, overrideReason becomes required.
     */
    private Boolean finalEligibleOverride;

    /** Required only when finalEligibleOverride disagrees with the system's verdict. */
    private String overrideReason;
}
