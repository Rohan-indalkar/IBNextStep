package com.infobeans.ibnextstep.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseRepository extends MongoRepository<Course, String> {
    Page<Course> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
