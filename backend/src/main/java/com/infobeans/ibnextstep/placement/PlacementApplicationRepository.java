package com.infobeans.ibnextstep.placement;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlacementApplicationRepository extends MongoRepository<PlacementApplication, String>,
        PlacementApplicationRepositoryCustom {

    Optional<PlacementApplication> findByPlacementIdAndStudentId(String placementId, String studentId);

    boolean existsByPlacementIdAndStudentId(String placementId, String studentId);

    List<PlacementApplication> findByStudentId(String studentId);

    List<PlacementApplication> findByStudentIdAndStatus(String studentId, PlacementApplicationStatus status);

    List<PlacementApplication> findByStatus(PlacementApplicationStatus status);

    long countByPlacementId(String placementId);

    long countByPlacementIdAndStatus(String placementId, PlacementApplicationStatus status);

    long countByStatus(PlacementApplicationStatus status);
}
