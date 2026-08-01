package com.infobeans.ibnextstep.quiz.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SubmitQuizRequest {
    /** Final answer set — same shape as autosave. If omitted, whatever was last auto-saved is used as-is. */
    private Map<String, List<String>> answers;
}
