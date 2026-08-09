package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/** "Every AI generated question should automatically be stored in Question Bank." */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "question_bank")
public class QuestionBankItem {

    @Id
    private String id;

    private Question question;

    private String topic;
    private List<String> subTopics;
    private String technology;
    private String company;
    private List<String> tags;

    private String generatedByTrainerId;
    /** true if AI-generated; false if a trainer manually added/edited it into the bank. */
    private boolean aiGenerated;

    private Instant createdAt;
}
