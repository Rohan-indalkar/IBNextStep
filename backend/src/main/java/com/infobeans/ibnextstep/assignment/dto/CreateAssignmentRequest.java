package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.material.PublishOption;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Sent as the "data" part of a multipart/form-data request (Content-Type:
 * application/json on that part), alongside an optional "files" part
 * carrying reference PDF(s). Used for both create and edit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRequest {

    @NotBlank(message = "Title is required")
    private String title;
    private String description;

    @NotBlank(message = "Course is required")
    private String courseId;
    private String module;
    private String topic;

    @NotNull(message = "Skill type is required")
    private TrainerType skillType;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficultyLevel;

    @Builder.Default
    private List<String> batchIds = List.of();

    /** May be empty if this assignment is purely a reference PDF. */
    @Valid
    @Builder.Default
    private List<AssignmentQuestionRequest> questions = List.of();

    private Instant dueDate;

    @NotNull(message = "Select a publish option: SAVE_AS_DRAFT, PUBLISH_NOW or SCHEDULE_PUBLISH")
    private PublishOption publishOption;

    /** Required when publishOption == SCHEDULE_PUBLISH. */
    private Instant scheduledAt;
}
