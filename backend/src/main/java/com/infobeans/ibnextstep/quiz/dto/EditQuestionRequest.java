package com.infobeans.ibnextstep.quiz.dto;

import com.infobeans.ibnextstep.quiz.Question;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EditQuestionRequest {
    @NotNull
    @Valid
    private Question question;
}
