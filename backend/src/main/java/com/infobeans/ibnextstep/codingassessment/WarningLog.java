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
@Document(collection = "warninglog")
public class WarningLog {

    @Id
    private String id;

    private String sessionId;
    private String assessmentId;
    private String studentId;

    private WarningType type;
    private int warningNumber;

    private Instant occurredAt;
}
