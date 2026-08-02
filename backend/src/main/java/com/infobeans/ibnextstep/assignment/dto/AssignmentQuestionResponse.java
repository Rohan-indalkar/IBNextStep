package com.infobeans.ibnextstep.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestionResponse {
    private String id;
    private String questionText;
    private int orderIndex;
}
