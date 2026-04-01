package com.adcheck.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI 응답 내 문장별 분석 결과
 */
@Getter
@NoArgsConstructor
public class SentenceResultDto {

    @JsonProperty("sentence")
    private String sentence;

    @JsonProperty("suspicion_level")
    private String suspicionLevel;  // "정상" | "주의" | "의심"

    @JsonProperty("matched_keywords")
    private List<String> matchedKeywords;

    @JsonProperty("reason")
    private String reason;
}
