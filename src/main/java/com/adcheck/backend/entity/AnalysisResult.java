package com.adcheck.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_result")
@Getter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비로그인이면 null, 로그인이면 user 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputType inputType;  // TEXT, URL, IMAGE

    @Column(columnDefinition = "TEXT")
    private String inputContent;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, length = 10)
    private String overallSuspicionLevel;  // "정상" | "주의" | "의심"

    private Double overallScore;  // 0.0 ~ 1.0

    @Column(columnDefinition = "TEXT")
    private String summary;

    // JSON → TEXT로 변경 (MySQL 버전 호환성)
    @Column(columnDefinition = "TEXT")
    private String sentenceResultsJson;

    @Column(nullable = false)
    private boolean hiddenFromHistory = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AnalysisResult(User user, InputType inputType, String inputContent,
                          String originalText, String overallSuspicionLevel,
                          Double overallScore, String summary, String sentenceResultsJson) {
        this.user = user;
        this.inputType = inputType;
        this.inputContent = inputContent;
        this.originalText = originalText;
        this.overallSuspicionLevel = overallSuspicionLevel;
        this.overallScore = overallScore;
        this.summary = summary;
        this.sentenceResultsJson = sentenceResultsJson;
        this.createdAt = LocalDateTime.now();
    }

    public void hideFromHistory() {
        this.hiddenFromHistory = true;
    }

    public enum InputType {
        TEXT, URL, IMAGE
    }
}
