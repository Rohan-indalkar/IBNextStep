package com.infobeans.ibnextstep.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/** One document per question per attempt — this is what powers "Question Wise Analysis" on the trainer dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "quiz_answer")
public class QuizAnswer {

    @Id
    private String id;

    private String attemptId;
    private String quizId;
    private String studentId;
    private String questionBankId;
    private String questionText;

    private List<String> studentAnswer;
    private boolean correct;
    private double marksAwarded;
    private boolean pendingManualReview;
}
