package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.assignment.Assignment;
import com.infobeans.ibnextstep.assignment.AssignmentStatus;
import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.material.StudyMaterial;
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
public class AssignmentResponse {
    private String id;
    private String title;
    private String description;

    private String courseId;
    private String courseName;
    private String module;
    private String topic;
    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;

    private List<String> batchIds;
    private List<String> batchNames;

    private List<AssignmentQuestionResponse> questions;
    private List<AssignmentFileResponse> referenceFiles;

    private boolean generatedByAI;

    private Instant dueDate;
    private AssignmentStatus status;
    private Instant scheduledAt;
    private Instant publishedAt;

    private String createdByTrainerId;
    private String createdByTrainerName;
    private Instant createdAt;
    private Instant updatedAt;

    public static AssignmentResponse.AssignmentResponseBuilder fromEntity(Assignment a) {
        List<AssignmentQuestionResponse> questions = a.getQuestions() == null ? List.of() : a.getQuestions().stream()
                .sorted((x, y) -> Integer.compare(x.getOrderIndex(), y.getOrderIndex()))
                .map(q -> AssignmentQuestionResponse.builder()
                        .id(q.getId()).questionText(q.getQuestionText()).orderIndex(q.getOrderIndex()).build())
                .toList();

        List<AssignmentFileResponse> files = a.getReferenceFiles() == null ? List.of() : a.getReferenceFiles().stream()
                .map(f -> AssignmentFileResponse.builder()
                        .fileId(f.getFileId()).fileName(f.getOriginalFileName())
                        .fileSizeBytes(f.getFileSizeBytes()).mimeType(f.getMimeType())
                        .downloadUrl("/api/trainer/assignments/" + a.getId() + "/files/" + f.getFileId() + "/download")
                        .build())
                .toList();

        return AssignmentResponse.builder()
                .id(a.getId()).title(a.getTitle()).description(a.getDescription())
                .courseId(a.getCourseId()).module(a.getModule()).topic(a.getTopic())
                .skillType(a.getSkillType()).difficultyLevel(a.getDifficultyLevel())
                .batchIds(a.getBatchIds())
                .questions(questions).referenceFiles(files)
                .generatedByAI(a.isGeneratedByAI())
                .dueDate(a.getDueDate()).status(a.getStatus())
                .scheduledAt(a.getScheduledAt()).publishedAt(a.getPublishedAt())
                .createdByTrainerId(a.getCreatedByTrainerId()).createdByTrainerName(a.getCreatedByTrainerName())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt());
    }
}
