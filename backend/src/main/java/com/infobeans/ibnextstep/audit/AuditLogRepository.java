package com.infobeans.ibnextstep.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByActionContainingIgnoreCaseOrUserEmailContainingIgnoreCase(
            String action, String userEmail, Pageable pageable);

    Page<AuditLog> findByUserId(String userId, Pageable pageable);
}
