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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
                .findByUserIdAndHiddenFromHistoryFalse(user.getId(), pageable)
                .map(HistoryItemDto::fromEntity);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHistoryById(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        return analysisResultRepository.findByIdAndHiddenFromHistoryFalse(id)
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
                .map(HistoryItemDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> hideMyHistory() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        int hiddenCount = analysisResultRepository.hideAllByUserId(user.getId());
        return ResponseEntity.ok(Map.of("hiddenCount", hiddenCount));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> hideHistoryById(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        int hiddenCount = analysisResultRepository.hideByIdAndUserId(id, user.getId());
        if (hiddenCount == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("hiddenCount", hiddenCount));
    }
}
