package com.adcheck.backend.controller;

import com.adcheck.backend.dto.HistoryItemDto;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 분석 이력 조회 엔드포인트(로그인 필요)
 *
 * GET /history          내 분석 이력 페이징 조회
 * GET /history/{id}     특정 이력 단건 조회
 */
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AnalysisResultRepository analysisResultRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<HistoryItemDto> result = analysisResultRepository
                .findByUserId(user.getId(), pageable)
                .map(HistoryItemDto::fromEntity);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHistoryById(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        return analysisResultRepository.findById(id)
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
                .map(HistoryItemDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
