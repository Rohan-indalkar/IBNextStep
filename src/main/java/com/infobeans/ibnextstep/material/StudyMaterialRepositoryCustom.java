package com.infobeans.ibnextstep.material;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudyMaterialRepositoryCustom {

    /** Builds a dynamic Criteria query — every parameter is optional (null/blank = "don't filter on this"). */
    Page<StudyMaterial> search(StudyMaterialSearchCriteria criteria, Pageable pageable);
}
