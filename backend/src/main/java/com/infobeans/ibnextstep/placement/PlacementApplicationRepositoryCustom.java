package com.infobeans.ibnextstep.placement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlacementApplicationRepositoryCustom {
    Page<PlacementApplication> search(PlacementApplicationSearchCriteria criteria, Pageable pageable);
}
