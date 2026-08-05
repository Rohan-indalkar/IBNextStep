package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Admin-editable override of the skill list scored for a given trainer
 * type. When no document exists for a TrainerType, the service falls back
 * to the built-in default rubric — so the feature is opt-in and the app
 * works unconfigured out of the box.
 * <p>
 * Changing this list going forward does NOT alter any already-submitted
 * StudentEvaluation records — each one keeps the skillScores map (and skill
 * names) it was submitted with, since that's a permanent snapshot in time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evaluation_rubric_configs")
public class EvaluationRubricConfig {

    @Id
    private String id;

    @Indexed(unique = true)
    private TrainerType trainerType;

    private List<String> skills;

    private String updatedByAdminId;
    private String updatedByAdminName;
    private Instant updatedAt;
}
