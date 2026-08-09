package com.infobeans.ibnextstep.mockinterview;

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
 * One record per student per interview slot. "Select Student(s)" on the
 * create form fans out into one MockInterview per student (same batch,
 * type, time and meeting link) so each gets an independent evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "mock_interviews")
public class MockInterview {

    @Id
    private String id;

    @Indexed
    private String batchId;

    @Indexed
    private String studentId;
    private String studentName;

    @Indexed
    private String trainerId;
    private String trainerName;

    private InterviewType interviewType;

    private Instant scheduledAt;
    @Builder.Default
    private int durationMinutes = 30;

    private String meetingLink;

    /** Trainer's note sent along with the invite email/notification. */
    private String notes;

    @Indexed
    private MockInterviewStatus status;

    private String cancellationReason;

    private Evaluation evaluation;

    private Instant publishedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evaluation {
        /** Parameter name -> score out of 10, e.g. "Problem Solving": 7, "Communication": 8. */
        @Builder.Default
        private java.util.Map<String, Integer> scores = java.util.Map.of();

        @Builder.Default
        private List<String> strengths = List.of();
        @Builder.Default
        private List<String> weaknesses = List.of();
        @Builder.Default
        private List<String> improvementSuggestions = List.of();
        private String additionalComments;

        /** Average of `scores`, out of 10, rounded to 1 decimal. */
        private Double overallRating;

        private Instant evaluatedAt;
        private String evaluatedByTrainerId;
    }
}
