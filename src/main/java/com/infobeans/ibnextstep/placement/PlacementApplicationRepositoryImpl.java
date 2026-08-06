package com.infobeans.ibnextstep.placement;

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
public class PlacementApplicationRepositoryImpl implements PlacementApplicationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<PlacementApplication> search(PlacementApplicationSearchCriteria criteria, Pageable pageable) {
        List<Criteria> filters = new ArrayList<>();

        if (criteria.getCompanyId() != null) {
            filters.add(Criteria.where("companyId").is(criteria.getCompanyId()));
        }
        if (criteria.getDepartmentId() != null) {
            filters.add(Criteria.where("departmentId").is(criteria.getDepartmentId()));
        }
        if (criteria.getPlacementId() != null) {
            filters.add(Criteria.where("placementId").is(criteria.getPlacementId()));
        }
        if (criteria.getStatus() != null) {
            filters.add(Criteria.where("status").is(criteria.getStatus()));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, PlacementApplication.class);
        query.with(pageable);
        List<PlacementApplication> content = mongoTemplate.find(query, PlacementApplication.class);

        return new PageImpl<>(content, pageable, total);
    }
}
