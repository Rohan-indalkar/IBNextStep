package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Shared shape used both in the permanent Question Bank and, as a snapshot
 * copy, inside each Quiz — so editing a bank question later never silently
 * changes a quiz that already referenced it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String questionText;

    /** 4 options for MCQ; 2 ("True"/"False") for TRUE_FALSE; N for MULTIPLE_SELECT; empty for FILL_BLANK/SHORT_ANSWER. */
    private List<String> options;

    /** For MCQ/TRUE_FALSE/FILL_BLANK: single value. For MULTIPLE_SELECT: comma-is-not-used, see correctAnswers below instead. */
    private String correctAnswer;

    /** Only populated for MULTIPLE_SELECT questions. */
    private List<String> correctAnswers;

    private String explanation;
    private QuestionType type;
    private Difficulty difficulty;
    private int marks;
}
