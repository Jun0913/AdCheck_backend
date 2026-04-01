package com.adcheck.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Spring Boot → FastAPI 분석 요청 DTO
 * POST /analyze/text
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequestDto {

    @JsonProperty("input_type")
    private String inputType;  // "text" | "url" | "image"

    @JsonProperty("content")
    private String content;
}
