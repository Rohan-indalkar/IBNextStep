package com.infobeans.ibnextstep.placement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlacementRepository extends MongoRepository<Placement, String> {

    Page<Placement> findByStatus(PlacementStatus status, Pageable pageable);

    Page<Placement> findByStatusAndType(PlacementStatus status, PlacementType type, Pageable pageable);

    List<Placement> findByStatus(PlacementStatus status);

    Page<Placement> findByCompanyId(String companyId, Pageable pageable);

    long countByStatus(PlacementStatus status);

    long countByType(PlacementType type);

    long countByStatusAndType(PlacementStatus status, PlacementType type);

    long countByCompanyId(String companyId);
}
