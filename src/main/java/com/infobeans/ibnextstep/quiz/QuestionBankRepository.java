package com.infobeans.ibnextstep.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface QuestionBankRepository extends MongoRepository<QuestionBankItem, String> {

    Page<QuestionBankItem> findByTopicIgnoreCase(String topic, Pageable pageable);

    Page<QuestionBankItem> findByQuestion_Difficulty(Difficulty difficulty, Pageable pageable);

    Page<QuestionBankItem> findByTechnologyIgnoreCase(String technology, Pageable pageable);

    Page<QuestionBankItem> findByCompanyIgnoreCase(String company, Pageable pageable);

    @Query("{ 'tags': { $in: [?0] } }")
    Page<QuestionBankItem> findByTag(String tag, Pageable pageable);
}
