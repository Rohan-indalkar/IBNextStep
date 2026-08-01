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
@Document(collection = "quiz_violation")
public class QuizViolation {

    @Id
    private String id;

    private String attemptId;
    private String quizId;
    private String studentId;

    private ViolationType type;
    /** Which warning number this was for its type (1st, 2nd, 3rd -> triggers auto-submit). */
    private int warningNumber;

    private Instant occurredAt;
}
