package com.infobeans.ibnextstep.quiz.dto;

import com.infobeans.ibnextstep.quiz.Difficulty;
import com.infobeans.ibnextstep.quiz.QuestionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class GenerateQuizRequest {

    private String title;

    @NotBlank
    private String prompt;

    @NotBlank
    private String topic;

    private List<String> subTopics;

    @NotNull
    private Difficulty difficulty;

    @Min(1) @Max(100)
    private int questionCount;

    @Min(1)
    private int durationMinutes;

    @DecimalMin("0") @DecimalMax("100")
    private double passingPercentage;

    @NotBlank
    private String batchId;

    private String language = "English";

    @NotEmpty
    private List<QuestionType> questionTypes;
}
