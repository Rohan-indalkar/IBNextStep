package com.infobeans.ibnextstep.quiz;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {
    Optional<QuizAttempt> findByQuizIdAndStudentId(String quizId, String studentId);
    List<QuizAttempt> findByQuizId(String quizId);
}
