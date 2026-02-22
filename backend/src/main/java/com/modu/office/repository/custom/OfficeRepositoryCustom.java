package com.modu.office.repository.custom;

import com.modu.office.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.modu.office.dto.request.OfficeSearchCondition;

public interface OfficeRepositoryCustom {
    Page<Office> searchOffices(OfficeSearchCondition condition, Pageable pageable);
}
