package com.adcheck.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI → Spring Boot 분석 결과 응답 DTO
 * POST /analyze/text, POST /analyze/image 공통 응답 형식
 */
@Getter
@NoArgsConstructor
public class AnalyzeResponseDto {

    @JsonProperty("original_text")
    private String originalText;

    @JsonProperty("overall_suspicion_level")
    private String overallSuspicionLevel;  // "정상" | "주의" | "의심"

    @JsonProperty("overall_score")
    private Double overallScore;  // 0.0 ~ 1.0

    @JsonProperty("sentence_results")
    private List<SentenceResultDto> sentenceResults;

    @JsonProperty("summary")
    private String summary;
}
