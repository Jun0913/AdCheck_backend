package com.adcheck.backend.repository;

import com.adcheck.backend.entity.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Page<AnalysisResult> findAll(Pageable pageable);

    Page<AnalysisResult> findByUserId(Long userId, Pageable pageable);

    Page<AnalysisResult> findByUserIdAndHiddenFromHistoryFalse(Long userId, Pageable pageable);

    Optional<AnalysisResult> findByIdAndHiddenFromHistoryFalse(Long id);

    @Modifying
    @Query("update AnalysisResult r set r.hiddenFromHistory = true where r.user.id = :userId")
    int hideAllByUserId(Long userId);

    @Modifying
    @Query("update AnalysisResult r set r.hiddenFromHistory = true where r.id = :id and r.user.id = :userId")
    int hideByIdAndUserId(Long id, Long userId);
}
