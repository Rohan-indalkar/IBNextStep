package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CodingQuestionRepository extends MongoRepository<CodingQuestion, String> {
    List<CodingQuestion> findByAssessmentIdOrderByOrderAsc(String assessmentId);
}
