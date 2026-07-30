package com.infobeans.ibnextstep.material;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface StudyMaterialRepository extends MongoRepository<StudyMaterial, String>, StudyMaterialRepositoryCustom {

    List<StudyMaterial> findByStatusAndScheduledAtBefore(MaterialStatus status, Instant instant);
}
