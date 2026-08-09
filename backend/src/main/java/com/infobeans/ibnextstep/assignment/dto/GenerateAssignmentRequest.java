package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** "Create assignment through AI". Lands as a DRAFT so the trainer can review/edit before publishing. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAssignmentRequest {

    @NotBlank(message = "Title is required")
    private String title;
    private String description;

    @NotBlank(message = "Course is required")
    private String courseId;
    private String module;

    @NotBlank(message = "Topic is required (this drives the AI prompt)")
    private String topic;

    @NotNull(message = "Skill type is required")
    private TrainerType skillType;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficultyLevel;

    @NotNull @Min(1) @Max(20)
    private Integer numberOfQuestions;

    private String additionalInstructions;

    private java.time.Instant dueDate;

    @Builder.Default
    private List<String> batchIds = List.of();
}
