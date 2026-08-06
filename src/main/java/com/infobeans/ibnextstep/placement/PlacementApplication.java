package com.infobeans.ibnextstep.placement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * One record per student who applies to a campus {@link Placement}.
 * Off-campus placements never create these — students just view/download/
 * open the external link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "placement_applications")
public class PlacementApplication {

    @Id
    private String id;

    @Indexed
    private String placementId;
    /** Denormalized so HR filters/lists don't need a join back to Placement for every row. */
    private String companyId;
    private String companyName;
    private String placementTitle;

    @Indexed
    private String studentId;
    private String studentName;
    private String studentEmail;
    /** Denormalized from User.departmentId at apply time — powers the HR "Department"/"Branch" filter. */
    private String departmentId;
    private String departmentName;

    @Indexed
    private PlacementApplicationStatus status;

    @Builder.Default
    private List<InterviewRoundInstance> rounds = List.of();

    /** Index into rounds of the round currently in progress; -1 if none scheduled yet. */
    @Builder.Default
    private int currentRoundIndex = -1;

    private Instant appliedAt;
    private Instant shortlistedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant selectedAt;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewRoundInstance {
        private int roundNumber;
        private String roundType;

        private Instant scheduledAt;
        private Integer durationMinutes;
        private String venue;
        private String meetingLink;
        private String remarks;

        @Builder.Default
        private InterviewRoundStatus status = InterviewRoundStatus.SCHEDULED;
        @Builder.Default
        private InterviewRoundResult result = InterviewRoundResult.PENDING;
        private String resultRemarks;

        private Instant updatedAt;
    }
}
