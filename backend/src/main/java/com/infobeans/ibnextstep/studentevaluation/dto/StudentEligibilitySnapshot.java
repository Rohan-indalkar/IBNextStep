package com.infobeans.ibnextstep.studentevaluation.dto;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * One row in the trainer's "pick a student to evaluate" batch roster.
 * Gives just enough at a glance — current auto-computed eligibility plus
 * whether (and how) this trainer's rubric type has already evaluated the
 * student — without requiring the trainer to already know the student's ID
 * or open each student individually.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEligibilitySnapshot {
    private String studentId;
    private String studentName;
    private String email;

    // ---------- Live system snapshot (recomputed on every load) ----------
    private Double attendancePercentage;
    private Double avgQuizPercentage;
    private Double avgCodingPercentage;
    private Double avgMockInterviewRating;
    private boolean systemEligible;
    private List<String> systemIneligibilityReasons;

    // ---------- Has THIS trainer's rubric type already evaluated this student? ----------
    private boolean evaluatedByMyRubric;
    private Instant lastEvaluatedAt;
    private Double lastOverallRubricScore;
    private Boolean lastFinalEligible;
    private String lastEvaluationId;

    /** Which rubric type this snapshot's "already evaluated" fields refer to — the viewing trainer's own type. */
    private TrainerType rubricType;
}
