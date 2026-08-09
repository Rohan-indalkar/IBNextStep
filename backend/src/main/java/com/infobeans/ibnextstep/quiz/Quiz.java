package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz")
public class Quiz {

    @Id
    private String id;

    private String title;
    private String prompt;
    private String topic;
    private List<String> subTopics;
    private Difficulty difficulty;
    private int questionCount;
    private int durationMinutes;
    private double passingPercentage;
    private String batchId;
    private String language;
    private List<QuestionType> questionTypes;

    /** Snapshot copies — editing these never touches the underlying Question Bank entry, and vice versa. */
    private List<QuizQuestionEntry> questions;

    private QuizStatus status;
    private Instant scheduledAt;

    private String trainerId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestionEntry {
        /** Traceability back to the Question Bank entry this was generated/copied from. Null if added manually with no bank record. */
        private String questionBankId;
        private Question question;
        private int order;
    }
}
