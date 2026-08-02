package com.infobeans.ibnextstep.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestionRequest {
    /** Present when editing an existing question, absent for a new one. */
    private String id;

    @NotBlank(message = "Question text is required")
    private String questionText;
}
