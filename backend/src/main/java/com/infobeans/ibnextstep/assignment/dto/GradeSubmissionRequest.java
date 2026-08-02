package com.infobeans.ibnextstep.assignment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Practice feedback — written comments + a 1-5 rating, no numeric score by design. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeSubmissionRequest {

    private String feedback;

    @NotNull(message = "Rating is required")
    @Min(1) @Max(5)
    private Integer rating;
}
