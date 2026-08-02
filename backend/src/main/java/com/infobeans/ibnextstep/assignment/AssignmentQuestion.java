package com.infobeans.ibnextstep.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One open-ended prompt inside an assignment — no options, no auto-grading (practice, not an exam). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestion {
    private String id;
    private String questionText;
    private int orderIndex;
}
