package com.modu.office.repository.custom;

import com.modu.office.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfficeRepositoryCustom {
    Page<Office> searchOffices(String keyword, Pageable pageable);
}
