package com.infobeans.ibnextstep.batch.dto;

import com.infobeans.ibnextstep.batch.Batch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * What a trainer is allowed to see about one of their own batches.
 * Deliberately narrower than the raw Batch document (e.g. no
 * studentIds list here — that's a separate roster call) so this
 * stays cheap to compute for the dashboard/overview screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerBatchSummaryResponse {

    private String id;
    private String name;
    private String courseId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Batch.BatchStatus status;

    private int studentCount;

    /** Which capacity this trainer serves on this batch — one or both can be true. */
    private boolean technicalTrainer;
    private boolean softSkillTrainer;

    public static TrainerBatchSummaryResponse from(Batch batch, String trainerId) {
        return TrainerBatchSummaryResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .courseId(batch.getCourseId())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .status(batch.getStatus())
                .studentCount(batch.getStudentIds() == null ? 0 : batch.getStudentIds().size())
                .technicalTrainer(trainerId.equals(batch.getTechnicalTrainerId()))
                .softSkillTrainer(trainerId.equals(batch.getSoftSkillTrainerId()))
                .build();
    }
}
