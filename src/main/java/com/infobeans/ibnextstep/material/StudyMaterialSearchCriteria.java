package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialSearchCriteria {
    /** Matched against title / description / topic (case-insensitive, partial). */
    private String search;
    private String createdByTrainerId;
    private String courseId;
    private String batchId;
    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;
    private ContentType contentType;
    private MaterialStatus status;
}
