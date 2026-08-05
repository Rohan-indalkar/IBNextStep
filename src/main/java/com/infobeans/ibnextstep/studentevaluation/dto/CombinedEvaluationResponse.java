package com.infobeans.ibnextstep.studentevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The student's latest Technical evaluation and latest Soft-Skill evaluation
 * side by side, plus a blended view across both rubrics. Either side may be
 * null if that rubric type hasn't evaluated the student yet — the combined
 * fields only average/require what's actually present.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinedEvaluationResponse {
    private String studentId;
    private String studentName;

    private StudentEvaluationResponse technical;
    private StudentEvaluationResponse softSkill;

    /** Average of technical.overallRubricScore and softSkill.overallRubricScore, whichever are present. Null if neither exists. */
    private Double combinedRubricScore;

    /**
     * True only when both sides exist and both mark the student finalEligible.
     * Null if one or both rubric types haven't evaluated the student yet —
     * an incomplete picture shouldn't be reported as a pass/fail verdict.
     */
    private Boolean combinedFinalEligible;

    private boolean hasTechnical;
    private boolean hasSoftSkill;
}
