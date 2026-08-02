package com.infobeans.ibnextstep.assignment;

import com.infobeans.ibnextstep.material.StudyMaterial;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    private String id;

    @Indexed
    private String assignmentId;

    @Indexed
    private String studentId;
    private String studentName;
    private String batchId;

    /** One entry per question; questionId is null for a general (PDF-only) submission. */
    @Builder.Default
    private List<AnswerEntry> answers = List.of();

    @Indexed
    private AssignmentSubmissionStatus status;
    private Instant submittedAt;

    // Practice feedback — no numeric score by design, just qualitative rating + comments.
    private String feedback;
    /** 1-5 stars. */
    private Integer rating;
    private Instant gradedAt;
    private String gradedByTrainerId;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerEntry {
        private String questionId;
        private String textAnswer;
        @Builder.Default
        private List<StudyMaterial.MaterialFile> files = List.of();
    }
}
