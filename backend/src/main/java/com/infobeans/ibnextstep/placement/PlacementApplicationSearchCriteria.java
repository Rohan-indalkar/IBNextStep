package com.infobeans.ibnextstep.placement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementApplicationSearchCriteria {
    private String companyId;
    private String departmentId;
    private String placementId;
    private PlacementApplicationStatus status;
}
