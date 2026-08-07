package com.infobeans.ibnextstep.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One student's outcome from an AI auto-review pass. {@code success=false}
 * means this particular resume failed (e.g. unreadable PDF) — used by the
 * bulk endpoint so one bad resume doesn't abort the whole batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoReviewResult {

    private String studentId;
    private String studentName;
    private boolean success;
    private String errorMessage;

    /** Null when success=false. */
    private ResumeStatus status;
    private Integer score;
    private ResumeAiAnalysis analysis;

    public static AutoReviewResult failed(String studentId, String studentName, String errorMessage) {
        return AutoReviewResult.builder()
                .studentId(studentId)
                .studentName(studentName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
