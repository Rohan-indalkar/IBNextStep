package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.PlacementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreatePlacementRequest {

    @NotBlank(message = "Company is required")
    private String companyId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Placement type is required")
    private PlacementType type;

    /** Only meaningful (and only applied) for CAMPUS drives. */
    @Valid
    private EligibilityCriteriaDto eligibility;

    private Instant applicationDeadline;

    /** Off-campus drives use this as their apply destination; campus drives may set it as a supplementary link. */
    private String externalApplyLink;

    @Valid
    private List<RoundTemplateDto> interviewRoundTemplates;

    private Double packageLpa;
}
