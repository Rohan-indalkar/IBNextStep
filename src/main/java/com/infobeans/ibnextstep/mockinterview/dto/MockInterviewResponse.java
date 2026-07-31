package com.infobeans.ibnextstep.mockinterview.dto;

import com.infobeans.ibnextstep.mockinterview.InterviewType;
import com.infobeans.ibnextstep.mockinterview.MockInterview;
import com.infobeans.ibnextstep.mockinterview.MockInterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewResponse {

    private String id;

    private String batchId;
    private String batchName;

    private String studentId;
    private String studentName;

    private String trainerId;
    private String trainerName;

    private InterviewType interviewType;
    private Instant scheduledAt;
    private int durationMinutes;
    private String meetingLink;
    private String notes;

    private MockInterviewStatus status;
    private String cancellationReason;

    private EvaluationDto evaluation;
    private Instant publishedAt;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationDto {
        private Map<String, Integer> scores;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> improvementSuggestions;
        private String additionalComments;
        private Double overallRating;
        private Instant evaluatedAt;
    }

    public static MockInterviewResponse.MockInterviewResponseBuilder fromEntity(MockInterview m) {
        EvaluationDto evaluationDto = null;
        if (m.getEvaluation() != null) {
            evaluationDto = EvaluationDto.builder()
                    .scores(m.getEvaluation().getScores())
                    .strengths(m.getEvaluation().getStrengths())
                    .weaknesses(m.getEvaluation().getWeaknesses())
                    .improvementSuggestions(m.getEvaluation().getImprovementSuggestions())
                    .additionalComments(m.getEvaluation().getAdditionalComments())
                    .overallRating(m.getEvaluation().getOverallRating())
                    .evaluatedAt(m.getEvaluation().getEvaluatedAt())
                    .build();
        }

        return MockInterviewResponse.builder()
                .id(m.getId())
                .batchId(m.getBatchId())
                .studentId(m.getStudentId())
                .studentName(m.getStudentName())
                .trainerId(m.getTrainerId())
                .trainerName(m.getTrainerName())
                .interviewType(m.getInterviewType())
                .scheduledAt(m.getScheduledAt())
                .durationMinutes(m.getDurationMinutes())
                .meetingLink(m.getMeetingLink())
                .notes(m.getNotes())
                .status(m.getStatus())
                .cancellationReason(m.getCancellationReason())
                .evaluation(evaluationDto)
                .publishedAt(m.getPublishedAt())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt());
    }
}
