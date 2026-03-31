# 딱 걸렸어! — API 명세서
**FastAPI 분석 서버 (Python) ↔ Spring Boot 백엔드 (Java) 연동용**

---

## 기본 정보

| 항목 | 값 |
|---|---|
| FastAPI 서버 주소 | `http://localhost:8000` |
| Spring Boot 서버 주소 | `http://localhost:8080` |
| 데이터 형식 | JSON (UTF-8) |
| 인증 방식 | 없음 (내부 서버 간 통신) |

---

## 공통 응답 형식

### 의심도 레벨 값
| 값 | 설명 |
|---|---|
| `"정상"` | 허위·과장 표현 없음 |
| `"주의"` | 과장 가능성 있는 표현 포함 |
| `"의심"` | 허위·과장 광고 의심 표현 포함 |

### 입력 타입 값
| 값 | 설명 |
|---|---|
| `"text"` | 텍스트 직접 입력 |
| `"url"` | 광고 URL 입력 |
| `"image"` | 이미지 파일 업로드 |

---

## API 목록

---

### 1. 서버 상태 확인

**Spring Boot → FastAPI**

```
GET /health
```

**응답 예시**
```json
{
  "status": "ok",
  "service": "딱 걸렸어! 분석 서버"
}
```

**Spring Boot 활용:**
서버 시작 시 또는 주기적으로 FastAPI 서버가 살아있는지 확인할 때 사용

---

### 2. 텍스트 / URL 분석 요청

**Spring Boot → FastAPI**

```
POST /analyze/text
Content-Type: application/json
```

**요청 Body**
```json
{
  "input_type": "text",
  "content": "분석할 광고 문구 텍스트"
}
```

**URL 입력 시**
```json
{
  "input_type": "url",
  "content": "https://example.com/product"
}
```

**응답 Body**
```json
{
  "original_text": "입력된 원본 텍스트 전체",
  "overall_suspicion_level": "의심",
  "overall_score": 0.85,
  "sentence_results": [
    {
      "sentence": "이 크림은 아토피를 치료합니다.",
      "suspicion_level": "의심",
      "matched_keywords": ["치료", "아토피"],
      "reason": "의약품으로 오인될 수 있는 표현이 포함되어 있습니다. (감지 키워드: 아토피, 치료)"
    },
    {
      "sentence": "피부 보습에 도움을 드립니다.",
      "suspicion_level": "정상",
      "matched_keywords": [],
      "reason": "특별히 의심되는 표현이 발견되지 않았습니다."
    }
  ],
  "summary": "총 2개 문장 중 1개에서 허위·과장 가능성이 높은 표현이 감지되었습니다."
}
```

**필드 설명**
| 필드 | 타입 | 설명 |
|---|---|---|
| `original_text` | String | 분석에 사용된 원본 텍스트 |
| `overall_suspicion_level` | String | 전체 의심도 레벨 (정상/주의/의심) |
| `overall_score` | float | 전체 의심도 점수 (0.0 ~ 1.0) |
| `sentence_results` | Array | 문장별 분석 결과 목록 |
| `sentence_results[].sentence` | String | 분석된 문장 |
| `sentence_results[].suspicion_level` | String | 문장별 의심도 레벨 |
| `sentence_results[].matched_keywords` | Array | 감지된 키워드 목록 |
| `sentence_results[].reason` | String | 의심 이유 설명 |
| `summary` | String | 전체 분석 요약 |

**에러 응답**
```json
{
  "detail": "URL에서 텍스트를 가져오지 못했습니다: ..."
}
```

| HTTP 상태코드 | 상황 |
|---|---|
| `200` | 분석 성공 |
| `400` | URL 크롤링 실패 |
| `422` | 요청 데이터 형식 오류 |

---

### 3. 이미지 분석 요청

**Spring Boot → FastAPI**

```
POST /analyze/image
Content-Type: multipart/form-data
```

**요청 Form-data**
| 키 | 타입 | 설명 |
|---|---|---|
| `file` | File | 이미지 파일 (jpg, png, gif 등) |

**Spring Boot 전송 예시 (Java)**
```java
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new FileSystemResource(imageFile));

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);

HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
ResponseEntity<String> response = restTemplate.postForEntity(
    pythonServerUrl + "/analyze/image",
    requestEntity,
    String.class
);
```

**응답 Body** — 텍스트 분석과 동일한 형식
```json
{
  "original_text": "OCR로 추출된 텍스트",
  "overall_suspicion_level": "주의",
  "overall_score": 0.5,
  "sentence_results": [...],
  "summary": "..."
}
```

**에러 응답**
| HTTP 상태코드 | 상황 |
|---|---|
| `200` | 분석 성공 |
| `400` | 이미지 파일이 아닌 파일 업로드 |
| `422` | 이미지에서 텍스트를 인식하지 못한 경우 |

---

## Spring Boot 연동 흐름

```
[React 프론트엔드]
        │
        │ POST /api/analyze/text  (또는 /url, /image)
        ▼
[Spring Boot :8080]
  1. 요청 수신 및 유효성 검사
  2. FastAPI로 분석 요청 전달
        │
        │ POST /analyze/text  (또는 /analyze/image)
        ▼
[FastAPI :8000]
  1. 텍스트 추출 (URL 크롤링 / OCR / 직접 입력)
  2. 문장 분리 (kss)
  3. 1차 규칙기반 엔진 분석
  4. 2차 KoBERT 모델 분석
  5. 결과 JSON 반환
        │
        ▼
[Spring Boot :8080]
  3. 분석 결과 DB 저장 (MySQL)
  4. React에 응답 반환
        │
        ▼
[React 프론트엔드]
  결과 화면 표시
```

---

## Spring Boot에서 FastAPI 호출 예시 (Java)

```java
// application.yml 에 설정된 FastAPI 서버 주소 사용
@Value("${python.server.url}")
private String pythonServerUrl;

// 텍스트 분석 요청
public PythonResponseDto analyzeText(String inputType, String content) {
    String url = pythonServerUrl + "/analyze/text";

    Map<String, String> requestBody = new HashMap<>();
    requestBody.put("input_type", inputType);
    requestBody.put("content", content);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

    ResponseEntity<PythonResponseDto> response = restTemplate.postForEntity(
        url, entity, PythonResponseDto.class
    );

    return response.getBody();
}
```

---

## 참고 — Swagger UI

FastAPI 서버 실행 후 아래 주소에서 API 직접 테스트 가능

- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`
