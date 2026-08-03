package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.CodingDifficulty;
import com.infobeans.ibnextstep.codingassessment.CodingQuestion;
import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** What a student sees — deliberately excludes hidden test cases entirely, not just their answers. "Hidden Test Cases must never be returned in Student APIs." */
@Data
@Builder
public class QuestionStudentView {
    private String id;
    private String title;
    private String problemStatement;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private List<CodingQuestion.Example> examples;
    private CodingDifficulty difficulty;
    private int marks;
    private int timeLimitSeconds;
    private int memoryLimitMb;
    private List<ProgrammingLanguage> allowedLanguages;
    private List<PublicTestCaseView> publicTestCases;
    private int order;

    @Data
    @Builder
    public static class PublicTestCaseView {
        private String id;
        private String input;
        private String expectedOutput;
    }
}
