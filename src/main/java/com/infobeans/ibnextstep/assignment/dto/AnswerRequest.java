package com.infobeans.ibnextstep.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One answer inside a submission. questionId is omitted/null for a general (non-per-question) answer. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {
    private String questionId;
    private String textAnswer;
}
