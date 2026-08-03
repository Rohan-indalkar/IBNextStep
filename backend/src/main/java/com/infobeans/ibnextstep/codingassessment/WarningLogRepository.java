package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WarningLogRepository extends MongoRepository<WarningLog, String> {
    List<WarningLog> findBySessionId(String sessionId);
}
