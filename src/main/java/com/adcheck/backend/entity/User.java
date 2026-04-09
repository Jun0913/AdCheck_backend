package com.adcheck.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")  // MySQL 예약어 'user' 충돌 방지
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;  // 소셜 로그인 시 null 가능

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;  // LOCAL(기본값), KAKAO, GOOGLE

    @Column
    private String providerId;  // 소셜 로그인 ID (LOCAL이면 null)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AnalysisResult> analysisResults = new ArrayList<>();

    @Builder
    public User(String email, String password, String nickname,
                Provider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.providerId = providerId;
        this.createdAt = LocalDateTime.now();
    }

    public enum Provider {
        LOCAL, KAKAO, GOOGLE
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
