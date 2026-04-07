package com.adcheck.backend.dto;

import com.adcheck.backend.entity.AnalysisResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * History response item for the frontend.
 * Strips user information and keeps field names the frontend already expects.
 */
@Getter
@Builder
public class HistoryItemDto {

    private Long id;
    private String inputType;
    private String inputContent;
    private String originalText;
    private String overallSuspicionLevel;
    private Double overallScore;
    private String summary;
    private String sentenceResultsJson;
    private LocalDateTime createdAt;

    public static HistoryItemDto fromEntity(AnalysisResult entity) {
        if (entity == null) return null;

        return HistoryItemDto.builder()
                .id(entity.getId())
                .inputType(entity.getInputType() != null ? entity.getInputType().name() : null)
                .inputContent(entity.getInputContent())
                .originalText(entity.getOriginalText())
                .overallSuspicionLevel(entity.getOverallSuspicionLevel())
                .overallScore(entity.getOverallScore())
                .summary(entity.getSummary())
                .sentenceResultsJson(entity.getSentenceResultsJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
