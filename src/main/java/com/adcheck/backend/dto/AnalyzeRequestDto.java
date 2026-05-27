package com.adcheck.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnalyzeRequestDto {

    @JsonProperty("content")
    private String content;

    public AnalyzeRequestDto(String content) {
        this.content = content;
    }
}
