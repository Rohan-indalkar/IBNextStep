package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.PlacementApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementApplicationResponse {
    private String id;
    private String placementId;
    private String placementTitle;
    private String companyId;
    private String companyName;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String departmentName;

    private PlacementApplicationStatus status;

    private List<InterviewRoundDto> rounds;
    private int currentRoundIndex;

    private Instant appliedAt;
    private Instant shortlistedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant selectedAt;
}
