# 담당자 B 컨텍스트 — 분석·이력 연동 마무리

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
│   ├── RestTemplateConfig.java     # RestTemplate 빈
│   ├── WebConfig.java              # CORS 설정 (3000, 5173 허용)
│   └── SecurityConfig.java         # ← A 담당자가 만듦 (인증 설정)
├── controller/
│   ├── AnalysisController.java     # ← B 담당 (TODO 연결 필요)
│   ├── HistoryController.java      # ← B 담당 (TODO 연결 필요)
│   └── UserController.java         # ← A 담당자가 만듦
├── dto/
│   ├── AnalyzeRequestDto.java      # FastAPI 요청용
│   ├── AnalyzeResponseDto.java     # FastAPI 응답용
│   └── SentenceResultDto.java      # 문장별 결과
├── entity/
│   ├── User.java                   # ← A 담당자가 만듦
│   └── AnalysisResult.java         # user 필드 포함 (nullable)
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── AnalysisResultRepository.java
│   └── UserRepository.java         # ← A 담당자가 만듦
└── service/
    ├── AnalysisService.java        # ← B 담당
    └── UserService.java            # ← A 담당자가 만듦
```

---

## B의 현재 작업 상태

**이미 완성된 것들 (건드리지 않아도 됨)**

- `AnalysisService.java` — FastAPI 호출, DB 저장 로직 완성
- `AnalysisResultRepository.java` — 전체 조회, 유저별 조회 완성
- `GlobalExceptionHandler.java` — 전역 에러 처리 완성
- `AnalyzeRequestDto`, `AnalyzeResponseDto`, `SentenceResultDto` — 완성

**B가 마무리해야 할 것들**

1. `AnalysisController.java` — TODO 주석 2곳 연결
2. `HistoryController.java` — TODO 주석 1곳 연결
3. 전체 흐름 테스트

---

## TODO 연결 방법 (A 완성 후 진행)

### 1. AnalysisController.java

현재 코드 (3곳 모두 동일):
```java
// TODO: 로그인 구현 후 @AuthenticationPrincipal User user 로 교체
User user = null;
```

A가 JWT 완성하면 아래처럼 교체:
```java
@PostMapping("/analyze/text")
public ResponseEntity<?> analyzeText(
        @RequestBody Map<String, String> body,
        @AuthenticationPrincipal User user  // 비로그인이면 null
) { ... }
```

### 2. HistoryController.java

현재 코드:
```java
// TODO: 로그인 구현 후 userId를 세션/토큰에서 가져오도록 교체
@RequestParam(required = false) Long userId
```

A가 JWT 완성하면 아래처럼 교체:
```java
@GetMapping
public ResponseEntity<?> getMyHistory(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    if (user == null) {
        return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
    }
    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ResponseEntity.ok(analysisResultRepository.findByUserId(user.getId(), pageable));
}
```

---

## FastAPI 분석 서버 연동 정보

Spring Boot → FastAPI 통신 구조:

```
POST /analyze/text  →  pythonServerUrl + "/analyze/text"
POST /analyze/image →  pythonServerUrl + "/analyze/image"
GET  /health        →  pythonServerUrl + "/health"
```

`application.properties`:
```properties
python.server.url=http://localhost:8000
```

FastAPI 응답 JSON 구조:
```json
{
  "original_text": "분석된 텍스트",
  "overall_suspicion_level": "의심",
  "overall_score": 0.85,
  "sentence_results": [
    {
      "sentence": "이 크림은 아토피를 치료합니다.",
      "suspicion_level": "의심",
      "matched_keywords": ["아토피", "치료"],
      "reason": "의약품으로 오인될 수 있는 표현입니다."
    }
  ],
  "summary": "총 2개 문장 중 1개에서..."
}
```

---

## DB 구조

### user 테이블 (A 담당자가 관리)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 자동 증가 |
| email | varchar | unique |
| password | varchar | BCrypt 해싱, 소셜 로그인 시 null |
| nickname | varchar | |
| provider | enum | LOCAL / KAKAO / GOOGLE |
| provider_id | varchar | 소셜 로그인 ID |
| created_at | datetime | |

### analysis_result 테이블 (B 담당자가 관리)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 자동 증가 |
| user_id | bigint FK | **nullable** — 비로그인이면 null |
| input_type | enum | TEXT / URL / IMAGE |
| input_content | text | 입력 원본값 |
| original_text | text | 실제 분석된 텍스트 |
| overall_suspicion_level | varchar | 정상 / 주의 / 의심 |
| overall_score | double | 0.0 ~ 1.0 |
| summary | text | 요약 설명 |
| sentence_results_json | json | 문장별 결과 JSON |
| created_at | datetime | |

---

## 분석 이력 저장 정책

- **비로그인** → `user = null` 로 저장 (통계용)
- **로그인** → `user` 연결하여 저장 (내 이력 조회 가능)
- 모든 분석은 항상 저장됨 (비로그인 포함)

---

## 테스트 시나리오 (A 완성 후 전체 흐름 테스트)

1. FastAPI 서버 실행 (`uvicorn analysis.main:app --port 8000`)
2. Spring Boot 서버 실행
3. 비로그인 상태로 텍스트 분석 → 결과 반환 + DB에 `user_id=null` 로 저장 확인
4. 로그인 후 텍스트 분석 → 결과 반환 + DB에 `user_id` 연결 확인
5. `GET /history` 호출 → 내 이력만 반환 확인

---

## 코드 컨벤션

- 패키지: `com.adcheck.backend`
- Lombok 사용 중 (`@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`)
- 에러 응답 형식: `Map.of("error", "메시지")` 로 통일
- `@RestController` + `@RequiredArgsConstructor` 패턴 사용

---

## 주의사항

- `AnalysisService.java`는 **건드리지 마세요** — A 작업과 충돌 가능성 없지만 완성된 파일이에요
- A의 `SecurityConfig`에서 `/analyze/**`와 `/health`는 인증 없이 허용으로 설정되어 있어요
- A의 JWT 작업이 완료되기 전까지는 TODO 주석 상태 그대로 두세요
- Git에서 항상 `git pull` 먼저 하고 작업 시작하세요
