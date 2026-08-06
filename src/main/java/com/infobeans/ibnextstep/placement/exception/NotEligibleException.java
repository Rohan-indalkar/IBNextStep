package com.infobeans.ibnextstep.placement.exception;

import com.infobeans.ibnextstep.placement.dto.EligibilityCheckResponse;
import lombok.Getter;

/**
 * Thrown when the backend re-validates a student's eligibility at apply
 * time and finds they no longer (or still don't) qualify. Carries the full
 * {@link EligibilityCheckResponse} — every failed rule, not just the first —
 * so the client can render each one, per the required student-facing flow.
 */
@Getter
public class NotEligibleException extends RuntimeException {

    private final EligibilityCheckResponse eligibility;

    public NotEligibleException(EligibilityCheckResponse eligibility) {
        super("You are not eligible to apply for this placement");
        this.eligibility = eligibility;
    }
}
