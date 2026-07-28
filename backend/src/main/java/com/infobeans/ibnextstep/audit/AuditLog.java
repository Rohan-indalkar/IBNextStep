package com.infobeans.ibnextstep.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog 
{

    @Id
    private String id;

    private String userId;
    private String userEmail;
    private String role;

    private String action;

    private String details;

    private String ipAddress;

    private Instant timestamp;
}
