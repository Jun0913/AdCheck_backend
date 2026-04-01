package com.adcheck.backend.controller;

import com.adcheck.backend.entity.AnalysisResult;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 분석 이력 조회 엔드포인트 (로그인 필요)
 *
 * GET /history          — 내 분석 이력 (최신순, 페이징)
 * GET /history/{id}     — 이력 단건 조회
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 내 분석 이력 목록
     * GET /history?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<?> getMyHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AnalysisResult> result = analysisResultRepository.findByUserId(user.getId(), pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 분석 이력 단건 조회
     * GET /history/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getHistoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        return analysisResultRepository.findById(id)
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
