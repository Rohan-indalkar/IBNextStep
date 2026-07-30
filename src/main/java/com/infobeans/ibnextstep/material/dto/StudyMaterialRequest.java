package com.infobeans.ibnextstep.material.dto;

import com.infobeans.ibnextstep.material.ContentType;
import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.material.PublishOption;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Sent as the "data" part of a multipart/form-data request
 * (Content-Type: application/json on that part), alongside a "files" part
 * carrying the actual uploads. See StudyMaterialController for the exact shape.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialRequest {

    // ---------- Basic Information ----------
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

    // ---------- Content Type ----------
    @NotNull(message = "Content type is required")
    private ContentType contentType;

    /** Required when contentType is VIDEO_LINK or EXTERNAL_RESOURCE_LINK. */
    private String externalUrl;

    // ---------- Access Control ----------
    @Builder.Default
    private List<String> batchIds = List.of();

    private LocalDate visibleFrom;
    private LocalDate expiryDate;

    // ---------- Publish Options ----------
    @NotNull(message = "Select a publish option: SAVE_AS_DRAFT, PUBLISH_NOW or SCHEDULE_PUBLISH")
    private PublishOption publishOption;

    /** Required when publishOption == SCHEDULE_PUBLISH. */
    private Instant scheduledAt;
}
