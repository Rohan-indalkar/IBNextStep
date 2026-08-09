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
public class CompanyResponse {
    private String id;
    private String name;
    private String description;
    private String industry;
    private String location;
    private String websiteUrl;
    private String logoPath;
    private boolean active;
    private String createdByHrName;
    private Instant createdAt;
    private Instant updatedAt;
}
