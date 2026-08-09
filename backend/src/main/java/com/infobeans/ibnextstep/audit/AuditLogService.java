package com.infobeans.ibnextstep.audit;

import com.infobeans.ibnextstep.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String userId, String userEmail, String role, String action, String details, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userEmail(userEmail)
                .role(role)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(entry);
    }

    public PagedResponse<AuditLog> search(String query, Pageable pageable) {
        String q = query == null ? "" : query;
        return PagedResponse.from(
                auditLogRepository.findByActionContainingIgnoreCaseOrUserEmailContainingIgnoreCase(q, q, pageable)
        );
    }
}
