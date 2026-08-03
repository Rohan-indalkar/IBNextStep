package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {
    List<ActivityLog> findBySessionId(String sessionId);
}
