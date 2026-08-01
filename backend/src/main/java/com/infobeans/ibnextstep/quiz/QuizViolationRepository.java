package com.infobeans.ibnextstep.quiz;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizViolationRepository extends MongoRepository<QuizViolation, String> {
    List<QuizViolation> findByAttemptId(String attemptId);
}
