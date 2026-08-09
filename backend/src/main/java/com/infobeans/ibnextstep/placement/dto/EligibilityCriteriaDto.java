package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/**
 * Every field is optional — HR only sets the rules that apply to a given
 * drive; an unset field means that rule isn't checked. CGPA / Branch /
 * Passing Year / Backlogs are intentionally not offered here: the platform
 * doesn't track student academic-profile data yet.
 */
@Data
public class EligibilityCriteriaDto {

    @DecimalMin(value = "0", message = "Minimum attendance % cannot be negative")
    @DecimalMax(value = "100", message = "Minimum attendance % cannot exceed 100")
    private Double minAttendancePercentage;

    @DecimalMin(value = "0", message = "Minimum quiz % cannot be negative")
    @DecimalMax(value = "100", message = "Minimum quiz % cannot exceed 100")
    private Double minQuizPercentage;

    @DecimalMin(value = "0", message = "Minimum coding % cannot be negative")
    @DecimalMax(value = "100", message = "Minimum coding % cannot exceed 100")
    private Double minCodingPercentage;

    @DecimalMin(value = "0", message = "Minimum mock interview rating cannot be negative")
    @DecimalMax(value = "10", message = "Minimum mock interview rating cannot exceed 10")
    private Double minMockInterviewRating;

    @DecimalMin(value = "0", message = "Minimum student evaluation score cannot be negative")
    @DecimalMax(value = "10", message = "Minimum student evaluation score cannot exceed 10")
    private Double minStudentEvaluationScore;

    private Boolean requireResumeApproved;
}
