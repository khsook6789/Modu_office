package com.modu.office.repository;

import com.modu.office.entity.CancellationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, Long> {

    /**
     * 특정 오피스의 환불 정책을 남은 일수(daysBefore) 기준 내림차순으로 조회합니다.
     */
    List<CancellationPolicy> findByOfficeIdOrderByDaysBeforeDesc(Long officeId);
}
