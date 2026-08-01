package com.infobeans.ibnextstep.quiz.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AutoSaveRequest {
    /** assignmentId -> selected answer(s) (multiple values only meaningful for MULTIPLE_SELECT). */
    @NotEmpty
    private Map<String, List<String>> answers;
}
