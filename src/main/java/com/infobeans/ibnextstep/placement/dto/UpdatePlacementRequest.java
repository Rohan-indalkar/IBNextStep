package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UpdatePlacementRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Valid
    private EligibilityCriteriaDto eligibility;

    private Instant applicationDeadline;

    private String externalApplyLink;

    @Valid
    private List<RoundTemplateDto> interviewRoundTemplates;

    private Double packageLpa;
}
