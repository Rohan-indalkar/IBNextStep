package com.infobeans.ibnextstep.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    Page<Quiz> findByTrainerId(String trainerId, Pageable pageable);
    List<Quiz> findByBatchIdAndStatus(String batchId, QuizStatus status);
}
