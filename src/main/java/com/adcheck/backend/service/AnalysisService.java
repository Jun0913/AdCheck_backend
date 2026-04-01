package com.adcheck.backend.service;

import com.adcheck.backend.dto.AnalyzeRequestDto;
import com.adcheck.backend.dto.AnalyzeResponseDto;
import com.adcheck.backend.entity.AnalysisResult;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.repository.AnalysisResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final AnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper;

    @Value("${python.server.url}")
    private String pythonServerUrl;

    // ──────────────────────────────────────────
    // 텍스트 / URL 분석
    // user: 로그인 상태면 User 객체, 비로그인이면 null
    // ──────────────────────────────────────────

    public AnalyzeResponseDto analyzeText(String inputType, String content, User user) {
        String url = pythonServerUrl + "/analyze/text";
        AnalyzeRequestDto requestDto = new AnalyzeRequestDto(inputType, content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalyzeRequestDto> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<AnalyzeResponseDto> response =
                    restTemplate.postForEntity(url, entity, AnalyzeResponseDto.class);
            AnalyzeResponseDto result = response.getBody();
            saveResult(user, AnalysisResult.InputType.valueOf(inputType.toUpperCase()), content, result);
            return result;
        } catch (RestClientException e) {
            log.error("Python 서버 통신 오류 (analyzeText): {}", e.getMessage());
            throw new RuntimeException("분석 서버와 통신할 수 없습니다.", e);
        }
    }

    // ──────────────────────────────────────────
    // 이미지 분석
    // ──────────────────────────────────────────

    public AnalyzeResponseDto analyzeImage(MultipartFile file, User user) throws Exception {
        String url = pythonServerUrl + "/analyze/image";

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AnalyzeResponseDto> response =
                    restTemplate.postForEntity(url, entity, AnalyzeResponseDto.class);
            AnalyzeResponseDto result = response.getBody();
            saveResult(user, AnalysisResult.InputType.IMAGE, file.getOriginalFilename(), result);
            return result;
        } catch (RestClientException e) {
            log.error("Python 서버 통신 오류 (analyzeImage): {}", e.getMessage());
            throw new RuntimeException("이미지 분석 서버와 통신할 수 없습니다.", e);
        }
    }

    // ──────────────────────────────────────────
    // Python 서버 헬스체크
    // ──────────────────────────────────────────

    public boolean checkPythonServerHealth() {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(pythonServerUrl + "/health", String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Python 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────
    // DB 저장
    // user == null 이면 비로그인 분석 (통계용으로는 저장, user_id = null)
    // user != null 이면 로그인 분석 (내 이력에서 조회 가능)
    // ──────────────────────────────────────────

    private void saveResult(User user, AnalysisResult.InputType inputType,
                            String inputContent, AnalyzeResponseDto result) {
        if (result == null) return;
        try {
            String sentenceJson = objectMapper.writeValueAsString(result.getSentenceResults());
            AnalysisResult entity = AnalysisResult.builder()
                    .user(user)
                    .inputType(inputType)
                    .inputContent(inputContent)
                    .originalText(result.getOriginalText())
                    .overallSuspicionLevel(result.getOverallSuspicionLevel())
                    .overallScore(result.getOverallScore())
                    .summary(result.getSummary())
                    .sentenceResultsJson(sentenceJson)
                    .build();
            analysisResultRepository.save(entity);
            log.info("분석 결과 저장 (user={}, inputType={}, level={})",
                    user != null ? user.getId() : "비로그인",
                    inputType, result.getOverallSuspicionLevel());
        } catch (JsonProcessingException e) {
            log.error("분석 결과 저장 실패: {}", e.getMessage());
        }
    }
}
