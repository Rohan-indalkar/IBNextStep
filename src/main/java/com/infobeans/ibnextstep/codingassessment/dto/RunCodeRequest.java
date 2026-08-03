package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunCodeRequest {
    @NotNull
    private ProgrammingLanguage language;

    @NotBlank
    private String code;
}
