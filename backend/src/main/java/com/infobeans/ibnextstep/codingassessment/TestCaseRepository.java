package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TestCaseRepository extends MongoRepository<TestCase, String> {
    List<TestCase> findByQuestionId(String questionId);
    List<TestCase> findByQuestionIdAndHidden(String questionId, boolean hidden);
}
