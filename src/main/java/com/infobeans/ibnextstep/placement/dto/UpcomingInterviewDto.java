package com.infobeans.ibnextstep.placement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingInterviewDto {
    private String applicationId;
    private String placementTitle;
    private String companyName;
    private String studentName;
    private int roundNumber;
    private String roundType;
    private Instant scheduledAt;
    private String venue;
    private String meetingLink;
}
