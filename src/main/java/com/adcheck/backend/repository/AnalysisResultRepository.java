package com.adcheck.backend.repository;

import com.adcheck.backend.entity.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    // 전체 이력 (관리자/통계용)
    Page<AnalysisResult> findAll(Pageable pageable);

    // 특정 회원의 이력 (내 이력 보기)
    Page<AnalysisResult> findByUserId(Long userId, Pageable pageable);
}
