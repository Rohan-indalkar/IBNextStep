package com.infobeans.ibnextstep.mockinterview;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MockInterviewRepositoryCustom {
    Page<MockInterview> search(MockInterviewSearchCriteria criteria, Pageable pageable);
}
