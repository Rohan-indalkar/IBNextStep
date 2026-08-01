package com.infobeans.ibnextstep.quiz.dto;

import lombok.Data;

@Data
public class RegenerateQuestionRequest {
    /** Optional extra instruction, e.g. "make it harder" or "focus on Spring Security instead". */
    private String additionalInstruction;
}
