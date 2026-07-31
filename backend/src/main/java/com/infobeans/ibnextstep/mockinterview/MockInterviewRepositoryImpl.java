package com.infobeans.ibnextstep.mockinterview;

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
public class MockInterviewRepositoryImpl implements MockInterviewRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<MockInterview> search(MockInterviewSearchCriteria criteria, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();

        if (criteria.getTrainerId() != null) {
            filters.add(Criteria.where("trainerId").is(criteria.getTrainerId()));
        }
        if (criteria.getBatchId() != null) {
            filters.add(Criteria.where("batchId").is(criteria.getBatchId()));
        }
        if (criteria.getStudentId() != null) {
            filters.add(Criteria.where("studentId").is(criteria.getStudentId()));
        }
        if (criteria.getInterviewType() != null) {
            filters.add(Criteria.where("interviewType").is(criteria.getInterviewType()));
        }
        if (criteria.getStatus() != null) {
            filters.add(Criteria.where("status").is(criteria.getStatus()));
        }
        if (criteria.getScheduledFrom() != null) {
            filters.add(Criteria.where("scheduledAt").gte(criteria.getScheduledFrom()));
        }
        if (criteria.getScheduledTo() != null) {
            filters.add(Criteria.where("scheduledAt").lte(criteria.getScheduledTo()));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, MockInterview.class);
        query.with(pageable);
        List<MockInterview> content = mongoTemplate.find(query, MockInterview.class);

        return new PageImpl<>(content, pageable, total);
    }
}
