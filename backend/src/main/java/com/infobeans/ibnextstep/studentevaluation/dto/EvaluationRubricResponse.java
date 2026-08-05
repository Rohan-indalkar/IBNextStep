package com.infobeans.ibnextstep.studentevaluation.dto;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRubricResponse {
    private TrainerType trainerType;
    /** Skill names to score, out of 10 each. */
    private List<String> skills;
}
