package com.infobeans.ibnextstep.mockinterview.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewEvaluationRequest {

    /** Parameter name -> score out of 10, e.g. {"Problem Solving": 7, "Communication": 8}. */
    @NotEmpty(message = "At least one scored parameter is required")
    private Map<String, Integer> scores;

    @Builder.Default
    private List<String> strengths = List.of();
    @Builder.Default
    private List<String> weaknesses = List.of();
    @Builder.Default
    private List<String> improvementSuggestions = List.of();
    private String additionalComments;
}
