package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.CodingDifficulty;
import com.infobeans.ibnextstep.codingassessment.CodingQuestion;
import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuestionRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String problemStatement;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private List<CodingQuestion.Example> examples;

    @NotNull
    private CodingDifficulty difficulty;
    @Min(1)
    private int marks;
    @Min(1)
    private int timeLimitSeconds = 2;
    @Min(16)
    private int memoryLimitMb = 256;
    @NotEmpty
    private List<ProgrammingLanguage> allowedLanguages;

    @NotEmpty
    private List<TestCaseInput> publicTestCases;
    private List<TestCaseInput> hiddenTestCases;

    @Data
    public static class TestCaseInput {
        @NotBlank
        private String input;
        @NotBlank
        private String expectedOutput;
    }
}
