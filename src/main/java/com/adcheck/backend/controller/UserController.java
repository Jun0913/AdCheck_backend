package com.adcheck.backend.controller;

import com.adcheck.backend.dto.LoginRequestDto;
import com.adcheck.backend.dto.LoginResponseDto;
import com.adcheck.backend.dto.RegisterRequestDto;
import com.adcheck.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 회원 관련 엔드포인트
 *
 * POST /register  — 회원가입
 * POST /login     — 로그인 → JWT 반환
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     * Body: { "email": "...", "password": "...", "nickname": "..." }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto dto) {
        try {
            userService.register(dto);
            return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 로그인
     * Body: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto dto) {
        try {
            LoginResponseDto response = userService.login(dto);
            // 편의: 토큰을 쿠키로도 내려줘서 프런트가 헤더를 못 붙이더라도 인증이 유지되도록 함
            ResponseCookie tokenCookie = ResponseCookie.from("token", response.getToken())
                    .httpOnly(true)
                    .secure(true)          // HTTPS 사용 시에만 전송
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(60L * 60 * 24 * 7) // 7일
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, tokenCookie.toString())
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
