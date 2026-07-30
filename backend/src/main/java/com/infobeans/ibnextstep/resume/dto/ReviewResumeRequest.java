package com.infobeans.ibnextstep.resume.dto;

import com.infobeans.ibnextstep.resume.ResumeStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewResumeRequest {

    @NotBlank
    private String suggestions;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private Double score;

    /** Only NEEDS_CHANGES or APPROVED are valid outcomes of a review — a trainer doesn't set PENDING_REVIEW themselves. */
    @NotNull
    private ResumeStatus status;

    @AssertTrue(message = "status must be either NEEDS_CHANGES or APPROVED")
    private boolean isValidStatus() {
        return status == ResumeStatus.NEEDS_CHANGES || status == ResumeStatus.APPROVED;
    }
}
