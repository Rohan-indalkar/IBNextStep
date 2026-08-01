package com.infobeans.ibnextstep.quiz;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends MongoRepository<QuizResult, String> {
    Optional<QuizResult> findByQuizIdAndStudentId(String quizId, String studentId);
    List<QuizResult> findByQuizId(String quizId);
    List<QuizResult> findByStudentId(String studentId);
}
