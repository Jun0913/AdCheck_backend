package com.adcheck.backend.controller;

import com.adcheck.backend.dto.AnalyzeResponseDto;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 프론트엔드 ↔ Spring Boot 분석 엔드포인트
 *
 * POST /analyze/text   — 텍스트 분석 (비로그인 가능)
 * POST /analyze/url    — URL 분석 (비로그인 가능)
 * POST /analyze/image  — 이미지 분석 (비로그인 가능)
 * GET  /health         — 서버 상태 확인
 *
 * 비로그인: user == null → 통계용으로만 저장 (user_id = null)
 * 로그인:   user 연결 → 내 이력에서 조회 가능
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @PostMapping("/analyze/text")
    public ResponseEntity<?> analyzeText(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content가 비어있습니다."));
        }
        try {
            AnalyzeResponseDto result = analysisService.analyzeText("text", content, getCurrentUser());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("텍스트 분석 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "분석 서버와 통신 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/analyze/url")
    public ResponseEntity<?> analyzeUrl(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "content가 비어있습니다."));
        }
        try {
            AnalyzeResponseDto result = analysisService.analyzeText("url", content, getCurrentUser());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("URL 분석 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "URL 분석 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/analyze/image")
    public ResponseEntity<?> analyzeImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        }
        try {
            AnalyzeResponseDto result = analysisService.analyzeImage(file, getCurrentUser());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("이미지 분석 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 분석 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean pythonOk = analysisService.checkPythonServerHealth();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "딱 걸렸어! Spring Boot 서버",
                "python_server", pythonOk ? "ok" : "unreachable"
        ));
    }
}
