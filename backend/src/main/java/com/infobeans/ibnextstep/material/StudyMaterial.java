package com.infobeans.ibnextstep.material;

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
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "study_materials")
public class StudyMaterial {

    @Id
    private String id;

    // ---------- Basic Information ----------
    private String title;
    private String description;

    @Indexed
    private String courseId;

    /** Free-text module label within the course (no separate Module catalog exists yet). */
    private String module;

    /** Free-text topic label within the module. */
    private String topic;

    /** Batches this material is assigned to. Empty while still a draft. */
    @Builder.Default
    @Indexed
    private List<String> batchIds = List.of();

    private TrainerType skillType;
    private DifficultyLevel difficultyLevel;

    // ---------- Content ----------
    private ContentType contentType;

    /** Uploaded files — supports multiple attachments per material. Empty for link-based content types. */
    @Builder.Default
    private List<MaterialFile> files = List.of();

    /** Used when contentType is VIDEO_LINK or EXTERNAL_RESOURCE_LINK. */
    private String externalUrl;

    // ---------- Access Control ----------
    private LocalDate visibleFrom;
    private LocalDate expiryDate;

    // ---------- Publish Options ----------
    @Indexed
    private MaterialStatus status;

    /** Set when status == SCHEDULED; MaterialPublishScheduler flips status to PUBLISHED once this passes. */
    private Instant scheduledAt;

    private Instant publishedAt;

    // ---------- Ownership / audit ----------
    @Indexed
    private String createdByTrainerId;
    private String createdByTrainerName;

    @Builder.Default
    private long downloadCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialFile {
        /** Random UUID, stable reference used in download URLs — never expose the disk path. */
        private String fileId;
        private String originalFileName;
        /** Path on disk relative to the configured storage root. */
        private String storedPath;
        private long fileSizeBytes;
        private String mimeType;
        private Instant uploadedAt;
    }
}
