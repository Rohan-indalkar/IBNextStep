package com.infobeans.ibnextstep.material.dto;

import com.infobeans.ibnextstep.material.ContentType;
import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.material.MaterialStatus;
import com.infobeans.ibnextstep.material.StudyMaterial;
import com.infobeans.ibnextstep.user.TrainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialResponse {

    private String id;
    private String title;
    private String description;

    private String courseId;
    private String courseName;
    private String module;
    private String topic;

    private List<String> batchIds;
    private List<String> batchNames;

    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;

    private ContentType contentType;
    private List<FileInfo> files;
    private String externalUrl;

    private LocalDate visibleFrom;
    private LocalDate expiryDate;

    private MaterialStatus status;
    /** Human-facing status that also accounts for visibleFrom/expiryDate (e.g. "EXPIRED", "UPCOMING"). */
    private String effectiveStatus;

    private Instant scheduledAt;
    private Instant publishedAt;

    private String createdByTrainerId;
    private String createdByTrainerName;
    private long downloadCount;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String fileId;
        private String fileName;
        private long fileSizeBytes;
        private String mimeType;
        private String downloadUrl;
    }

    public static StudyMaterialResponse.StudyMaterialResponseBuilder fromEntity(StudyMaterial m) {
        List<FileInfo> fileInfos = m.getFiles() == null ? List.of() : m.getFiles().stream()
                .map(f -> FileInfo.builder()
                        .fileId(f.getFileId())
                        .fileName(f.getOriginalFileName())
                        .fileSizeBytes(f.getFileSizeBytes())
                        .mimeType(f.getMimeType())
                        .downloadUrl("/api/trainer/study-materials/" + m.getId() + "/files/" + f.getFileId() + "/download")
                        .build())
                .toList();

        return StudyMaterialResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .courseId(m.getCourseId())
                .module(m.getModule())
                .topic(m.getTopic())
                .batchIds(m.getBatchIds())
                .skillType(m.getSkillType())
                .difficultyLevel(m.getDifficultyLevel())
                .contentType(m.getContentType())
                .files(fileInfos)
                .externalUrl(m.getExternalUrl())
                .visibleFrom(m.getVisibleFrom())
                .expiryDate(m.getExpiryDate())
                .status(m.getStatus())
                .scheduledAt(m.getScheduledAt())
                .publishedAt(m.getPublishedAt())
                .createdByTrainerId(m.getCreatedByTrainerId())
                .createdByTrainerName(m.getCreatedByTrainerName())
                .downloadCount(m.getDownloadCount())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt());
    }
}
