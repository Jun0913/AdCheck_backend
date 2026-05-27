package com.adcheck.backend.controller;

import com.adcheck.backend.dto.AnalyzeResponseDto;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.service.AnalysisService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            AnalyzeResponseDto result = analysisService.analyzeText(content, getCurrentUser());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("텍스트 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "분석 서버 통신 중 오류가 발생했습니다."));
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
            log.error("이미지 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "이미지 분석 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean pythonOk = analysisService.checkPythonServerHealth();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "광고체크 Spring Boot 서버",
                "python_server", pythonOk ? "ok" : "unreachable"
        ));
    }
}
