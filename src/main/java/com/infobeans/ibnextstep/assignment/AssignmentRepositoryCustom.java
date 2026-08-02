package com.infobeans.ibnextstep.assignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentRepositoryCustom {
    Page<Assignment> search(AssignmentSearchCriteria criteria, Pageable pageable);
}
