package com.infobeans.ibnextstep.material;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudyMaterialRepositoryImpl implements StudyMaterialRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<StudyMaterial> search(StudyMaterialSearchCriteria criteria, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();

        if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
            String regex = ".*" + java.util.regex.Pattern.quote(criteria.getSearch().trim()) + ".*";
            filters.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex, "i"),
                    Criteria.where("description").regex(regex, "i"),
                    Criteria.where("topic").regex(regex, "i")
            ));
        }
        if (criteria.getCreatedByTrainerId() != null) {
            filters.add(Criteria.where("createdByTrainerId").is(criteria.getCreatedByTrainerId()));
        }
        if (criteria.getCourseId() != null) {
            filters.add(Criteria.where("courseId").is(criteria.getCourseId()));
        }
        if (criteria.getBatchId() != null) {
            filters.add(Criteria.where("batchIds").in(criteria.getBatchId()));
        }
        if (criteria.getSkillType() != null) {
            filters.add(Criteria.where("skillType").is(criteria.getSkillType()));
        }
        if (criteria.getDifficultyLevel() != null) {
            filters.add(Criteria.where("difficultyLevel").is(criteria.getDifficultyLevel()));
        }
        if (criteria.getContentType() != null) {
            filters.add(Criteria.where("contentType").is(criteria.getContentType()));
        }
        if (criteria.getStatus() != null) {
            filters.add(Criteria.where("status").is(criteria.getStatus()));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, StudyMaterial.class);
        query.with(pageable);
        List<StudyMaterial> content = mongoTemplate.find(query, StudyMaterial.class);

        return new PageImpl<>(content, pageable, total);
    }
}
