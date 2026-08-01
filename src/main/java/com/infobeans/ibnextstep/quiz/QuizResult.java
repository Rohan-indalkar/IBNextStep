package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_result")
public class QuizResult {

    @Id
    private String id;

    private String quizId;
    private String studentId;
    private String attemptId;

    private int totalMarks;
    private double obtainedMarks;
    private int correctAnswers;
    private int wrongAnswers;
    private int pendingManualGrading;

    private double percentage;
    private boolean passed;

    private Instant startedAt;
    private Instant submittedAt;
    private long durationTakenSeconds;

    private int cheatingScore;
    private boolean needsReview;
}
