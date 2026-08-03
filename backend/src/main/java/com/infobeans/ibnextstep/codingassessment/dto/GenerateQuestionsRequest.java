package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.CodingDifficulty;
import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateQuestionsRequest {
    @NotBlank
    private String topic;

    @NotNull
    private ProgrammingLanguage language;

    /** null/omitted difficulty means "Mixed" per spec. */
    private CodingDifficulty difficulty;

    @Min(1) @Max(20)
    private int questionCount;
}
