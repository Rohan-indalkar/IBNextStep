package com.infobeans.ibnextstep.course;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SkillRepository extends MongoRepository<Skill, String> {
    boolean existsByNameIgnoreCase(String name);
    List<Skill> findByIdIn(List<String> ids);
}
