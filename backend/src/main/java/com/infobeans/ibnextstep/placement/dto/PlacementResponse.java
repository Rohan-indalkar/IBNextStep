package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.PlacementStatus;
import com.infobeans.ibnextstep.placement.PlacementType;
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
public class PlacementResponse {
    private String id;
    private String companyId;
    private String companyName;
    private String companyLogoPath;
    private String title;
    private String description;
    private PlacementType type;
    private PlacementStatus status;
    private EligibilityCriteriaDto eligibility;
    private Instant applicationDeadline;
    private String pdfPath;
    private String pdfFileName;
    private String externalApplyLink;
    private List<RoundTemplateDto> interviewRoundTemplates;
    private Double packageLpa;
    private String createdByHrName;
    private Instant publishedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Populated only on the HR list/detail view — the applications pipeline snapshot for this drive.
    private Long totalApplications;
    private Long shortlisted;
    private Long selected;
}
