package com.infobeans.ibnextstep.mockinterview;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MockInterviewRepository extends MongoRepository<MockInterview, String>, MockInterviewRepositoryCustom {

    List<MockInterview> findByStudentIdAndStatus(String studentId, MockInterviewStatus status);
}
