package com.infobeans.ibnextstep.placement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlacementOpportunityRepository extends MongoRepository<PlacementOpportunity, String> {
    Page<PlacementOpportunity> findByStatus(PlacementOpportunity.OpportunityStatus status, Pageable pageable);
}
