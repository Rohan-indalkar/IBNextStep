package com.infobeans.ibnextstep.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BatchRepository extends MongoRepository<Batch, String> {
    Page<Batch> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Batch> findByStudentIdsContaining(String studentId);
    List<Batch> findByTechnicalTrainerIdOrSoftSkillTrainerId(String trainerId1, String trainerId2);
}
