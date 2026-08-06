package com.infobeans.ibnextstep.studentevaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A student's raw, threshold-free metrics pulled live from the existing
 * attendance / quiz / coding assessment / mock interview modules.
 * <p>
 * Deliberately carries no eligibility verdict — different callers apply
 * different thresholds against the same numbers (e.g. {@link StudentEvaluationService}
 * uses the platform-wide default thresholds for trainer evaluations, while the
 * Placement module evaluates the same numbers against thresholds an HR user
 * configures per placement drive). Keeping the verdict out of this class is
 * what lets both reuse the same computation without duplicating it.
 * <p>
 * A null field means "no data yet" (e.g. student has no quiz attempts), which
 * callers should generally treat as not-yet-eligible rather than silently skipped.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRawMetrics {

    /** Percentage of attendance records marked PRESENT or LATE. Null if no records yet. */
    private Double attendancePercentage;

    /** Average of QuizResult.percentage across all the student's quiz attempts. Null if none yet. */
    private Double avgQuizPercentage;

    /** Average % (best marksAwarded / question.marks) across the student's best submission per coding question. Null if none yet. */
    private Double avgCodingPercentage;

    /** Average of Evaluation.overallRating (out of 10) across the student's published mock interviews. Null if none yet. */
    private Double avgMockInterviewRating;
}
