package com.infobeans.ibnextstep.assignment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String>, AssignmentRepositoryCustom {
    List<Assignment> findByStatusAndScheduledAtBefore(AssignmentStatus status, Instant instant);
}
