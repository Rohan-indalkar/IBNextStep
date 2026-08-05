package com.infobeans.ibnextstep.studentevaluation.dto;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRubricConfigResponse {
    private TrainerType trainerType;
    private List<String> skills;
    /** False if this is just the built-in default — no admin override saved yet. */
    private boolean customized;
    private Instant updatedAt;
    private String updatedByAdminName;
}
