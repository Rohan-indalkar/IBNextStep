package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.user.TrainerType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EvaluationRubricConfigRepository extends MongoRepository<EvaluationRubricConfig, String> {
    Optional<EvaluationRubricConfig> findByTrainerType(TrainerType trainerType);
}
