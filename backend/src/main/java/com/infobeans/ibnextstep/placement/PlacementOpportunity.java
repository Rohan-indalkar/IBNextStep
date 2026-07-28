package com.infobeans.ibnextstep.placement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * NOTE: This is a minimal stub to support the Admin Approvals workflow.
 * Full fields (eligibility criteria, date/venue, applications, results, etc.)
 * are added when the HR module is built.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "placement_opportunities")
public class PlacementOpportunity {

    @Id
    private String id;

    private String companyId;
    private String title;

    private OpportunityType type;
    private OpportunityStatus status;

    private String createdByHrId;

    /** Populated only when status == REJECTED */
    private String rejectionReason;

    @CreatedDate
    private Instant createdAt;

    public enum OpportunityType {
        CAMPUS_DRIVE,
        EXTERNAL
    }

    public enum OpportunityStatus {
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        PUBLISHED
    }
}
