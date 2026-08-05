package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One record per evaluation a trainer submits for a student. A student can
 * be evaluated more than once over time (e.g. monthly) — each submission is
 * its own permanent, dated record. Placement eligibility elsewhere in the
 * app always reads the MOST RECENT evaluation for a student.
 * <p>
 * A trainer can go back and correct a mistake on an evaluation they
 * submitted via the update endpoint — this edits the SAME record in place
 * (evaluatedAt stays the original submission time; updatedAt/edited track
 * the correction) rather than creating a new one, so it doesn't masquerade
 * as a fresh, independent evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "student_evaluations")
public class StudentEvaluation {

    @Id
    private String id;

    @Indexed
    private String studentId;
    private String batchId;

    private String trainerId;
    private String trainerName;

    /** Which rubric was used (Technical or Soft Skill) — matches the trainer's own type. */
    private TrainerType trainerType;

    // ---------- Metrics snapshot at evaluation time (pulled from existing modules) ----------
    /** Null if the student has no attendance records yet. */
    private Double attendancePercentage;
    /** Average of QuizResult.percentage across all the student's quiz attempts. Null if none yet. */
    private Double avgQuizPercentage;
    /** Average % (marksAwarded / question.marks) across the student's best submission per coding question. Null if none yet. */
    private Double avgCodingPercentage;
    /** Average of Evaluation.overallRating (out of 10) across the student's published mock interviews. Null if none yet. */
    private Double avgMockInterviewRating;

    // ---------- System's automatic eligibility computation ----------
    private boolean systemEligible;
    @Builder.Default
    private List<String> systemIneligibilityReasons = List.of();

    // ---------- Trainer's own rubric scoring ----------
    /** Skill name -> score out of 10, e.g. "Code Quality": 7. Rubric depends on trainerType. */
    private Map<String, Integer> skillScores;
    /** Average of skillScores, out of 10. */
    private Double overallRubricScore;

    private String remarks;

    // ---------- Final decision (trainer can agree with or override the system) ----------
    private boolean finalEligible;
    /** Required only when finalEligible != systemEligible. */
    private String overrideReason;

    private Instant evaluatedAt;

    // ---------- Edit tracking (evaluations are correctable, not immutable) ----------
    @Builder.Default
    private boolean edited = false;
    private Instant updatedAt;
    /** Name of the trainer who last edited this record (always the original trainer). */
    private String lastEditedBy;

    @CreatedDate
    private Instant createdAt;
}
