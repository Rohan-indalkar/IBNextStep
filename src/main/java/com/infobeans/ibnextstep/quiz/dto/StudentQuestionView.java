package com.infobeans.ibnextstep.quiz.dto;

import com.infobeans.ibnextstep.quiz.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** What a student sees while attempting — deliberately has NO correct-answer field, so it's impossible to leak the answer even by accident. */
@Data
@Builder
@AllArgsConstructor
public class StudentQuestionView {
    private String assignmentId;
    private String questionText;
    private List<String> options;
    private QuestionType type;
    private int marks;
    private int order;
}
