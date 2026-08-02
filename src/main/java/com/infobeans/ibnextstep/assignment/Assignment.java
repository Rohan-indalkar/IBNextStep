package com.infobeans.ibnextstep.assignment;

import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.material.StudyMaterial;
import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A practice assignment — trainer supplies either typed interview-style
 * questions (manually or AI-generated via Gemini), a reference PDF, or
 * both. Reuses StudyMaterial.MaterialFile for attachments so file storage
 * behaves identically to the Study Material module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;

    private String title;
    private String description;

    private String courseId;
    private String module;
    private String topic;
    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;

    @Builder.Default
    @Indexed
    private List<String> batchIds = List.of();

    /** Typed prompts — may be empty if the assignment is purely a reference PDF. */
    @Builder.Default
    private List<AssignmentQuestion> questions = List.of();

    /** Reference material (e.g. a PDF of interview questions) trainer attached. */
    @Builder.Default
    private List<StudyMaterial.MaterialFile> referenceFiles = List.of();

    private boolean generatedByAI;
    private String aiPrompt;

    private Instant dueDate;

    @Indexed
    private AssignmentStatus status;
    private Instant scheduledAt;
    private Instant publishedAt;

    @Indexed
    private String createdByTrainerId;
    private String createdByTrainerName;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
