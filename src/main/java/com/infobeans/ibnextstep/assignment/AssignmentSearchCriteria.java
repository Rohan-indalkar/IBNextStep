package com.infobeans.ibnextstep.assignment;

import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSearchCriteria {
    private String search;
    private String createdByTrainerId;
    private String courseId;
    private String batchId;
    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;
    private AssignmentStatus status;
}
