package com.infobeans.ibnextstep.codingassessment;

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
@Document(collection = "activitylog")
public class ActivityLog {

    @Id
    private String id;

    private String sessionId;
    private String assessmentId;
    private String studentId;

    private ActivityAction action;
    private String details;

    private Instant timestamp;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
}
