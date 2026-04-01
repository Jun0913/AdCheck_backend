# 담당자 A 컨텍스트 — 인증/회원 기능

> Claude Code에서 이 파일을 읽은 뒤 작업을 시작하세요.

---

## 프로젝트 개요

**딱 걸렸어!** — 화장품 광고 허위·과장 표현 의심도 분석 웹 서비스

- React (프론트) ↔ Spring Boot 8080 (백엔드) ↔ FastAPI 8000 (Python 분석 서버)
- DB: MySQL (`adcheck` 데이터베이스)

---

## 프로젝트 구조

```
src/main/java/com/adcheck/backend/
├── BackendApplication.java
├── config/
│   ├── RestTemplateConfig.java   # RestTemplate 빈
│   └── WebConfig.java            # CORS 설정 (3000, 5173 허용), ObjectMapper 빈
├── controller/
│   ├── AnalysisController.java   # /analyze/text, /analyze/url, /analyze/image
│   └── HistoryController.java    # /history, /history/{id}
├── dto/
│   ├── AnalyzeRequestDto.java
│   ├── AnalyzeResponseDto.java
│   └── SentenceResultDto.java
├── entity/
│   ├── User.java                 # ← A 담당
│   └── AnalysisResult.java       # user 필드 포함 (ManyToOne)
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   └── AnalysisResultRepository.java
└── service/
    └── AnalysisService.java      # analyzeText(inputType, content, user) — user는 null 가능
```

---

## A가 새로 만들어야 할 파일

```
config/
  SecurityConfig.java         ← Spring Security 설정
  JwtUtil.java                ← JWT 발급·검증 유틸
  JwtFilter.java              ← 요청마다 토큰 확인 필터

repository/
  UserRepository.java         ← 이메일로 유저 조회

service/
  UserService.java            ← 회원가입, 로그인 로직

controller/
  UserController.java         ← POST /register, POST /login

dto/
  RegisterRequestDto.java     ← { email, password, nickname }
  LoginRequestDto.java        ← { email, password }
  LoginResponseDto.java       ← { token, nickname }
```

---

## 이미 만들어진 User 엔티티

```java
// entity/User.java
public class User {
    private Long id;
    private String email;       // unique, not null
    private String password;    // 소셜 로그인 시 null 가능
    private String nickname;
    private Provider provider;  // LOCAL(기본값), KAKAO, GOOGLE
    private String providerId;  // 소셜 로그인 ID (LOCAL이면 null)
    private LocalDateTime createdAt;

    public enum Provider { LOCAL, KAKAO, GOOGLE }
}
```

---

## pom.xml에 추가해야 할 의존성

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

---

## application.properties에 추가할 설정

```properties
# JWT
jwt.secret=your_jwt_secret_key_here_minimum_32_characters
jwt.expiration=86400000
```

---

## SecurityConfig 작성 시 핵심 주의사항

아래 경로는 **인증 없이 허용** 해야 해요.
B 담당자가 만든 분석 API가 비로그인도 사용 가능해야 하기 때문이에요.

```java
// 인증 없이 허용할 경로
.requestMatchers("/analyze/**").permitAll()
.requestMatchers("/health").permitAll()
.requestMatchers("/register", "/login").permitAll()
// 나머지
.anyRequest().authenticated()
```

---

## B 담당자와의 연결 지점

A가 완성해야 B가 마무리할 수 있는 부분이 있어요.

`AnalysisController.java`와 `HistoryController.java`에 아래 TODO 주석이 있어요.
A가 JWT + Security 완성하면 B가 이 부분을 연결해요.

```java
// AnalysisController.java 내부
// TODO: 로그인 구현 후 @AuthenticationPrincipal User user 로 교체
User user = null;

// HistoryController.java 내부
// TODO: 로그인 구현 후 userId를 세션/토큰에서 가져오도록 교체
@RequestParam(required = false) Long userId
```

B가 쓸 수 있도록 `JwtUtil`에 토큰에서 userId 추출하는 메서드를 꼭 만들어두세요.

---

## 엔드포인트 정의

A가 만들 엔드포인트:

| Method | Path | 설명 | 인증 필요 |
|---|---|---|---|
| POST | `/register` | 회원가입 | 없음 |
| POST | `/login` | 로그인 → JWT 반환 | 없음 |

**회원가입 요청/응답**
```json
// 요청
{ "email": "user@example.com", "password": "password123", "nickname": "홍길동" }

// 응답 200
{ "message": "회원가입이 완료되었습니다." }

// 응답 409 (이미 존재하는 이메일)
{ "error": "이미 사용 중인 이메일입니다." }
```

**로그인 요청/응답**
```json
// 요청
{ "email": "user@example.com", "password": "password123" }

// 응답 200
{ "token": "eyJhbGci...", "nickname": "홍길동" }

// 응답 401
{ "error": "이메일 또는 비밀번호가 올바르지 않습니다." }
```

---

## 코드 컨벤션

- 패키지: `com.adcheck.backend`
- Lombok 사용 중 (`@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
- 에러 응답 형식: `Map.of("error", "메시지")` 로 통일
- `@RestController` + `@RequiredArgsConstructor` 패턴 사용
- 비밀번호는 반드시 **BCrypt** 로 해싱
