package com.infobeans.ibnextstep.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * One document per student. Every re-upload after "Needs Changes" appends a
 * new ResumeVersion rather than overwriting — so the full review history
 * (suggestions, score, who reviewed it, when) is preserved across rounds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resumes")
public class Resume {

    @Id
    private String id;

    private String studentId;

    /** Status of the LATEST version — mirrors versions.get(versions.size()-1).status for fast filtering/queries. */
    private ResumeStatus currentStatus;

    private List<ResumeVersion> versions;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeVersion {
        private int versionNumber;
        private String filePath;
        private String fileName;
        private Instant uploadedAt;

        // Review outcome for THIS specific version — null until a trainer reviews it.
        private String suggestions;
        private Double score;
        private ResumeStatus status;
        private String reviewedByTrainerId;
        private String reviewedByTrainerName;
        private Instant reviewedAt;
    }
}
