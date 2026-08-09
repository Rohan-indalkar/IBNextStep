package com.infobeans.ibnextstep.resume;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends MongoRepository<Resume, String> {
    Optional<Resume> findByStudentId(String studentId);
    List<Resume> findByCurrentStatus(ResumeStatus status);
}
