package com.infobeans.ibnextstep.quiz;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizAnswerRepository extends MongoRepository<QuizAnswer, String> {
    List<QuizAnswer> findByAttemptId(String attemptId);
    List<QuizAnswer> findByQuizId(String quizId);
}
