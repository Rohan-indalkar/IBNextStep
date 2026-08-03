package com.infobeans.ibnextstep.codingassessment;

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
@Document(collection = "codingquestion")
public class CodingQuestion {

    @Id
    private String id;

    private String assessmentId;

    private String title;
    private String problemStatement;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private List<Example> examples;

    private CodingDifficulty difficulty;
    private int marks;
    private int timeLimitSeconds;
    private int memoryLimitMb;
    private List<ProgrammingLanguage> allowedLanguages;

    private int order;

    private boolean aiGenerated;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String input;
        private String output;
        private String explanation;
    }
}
