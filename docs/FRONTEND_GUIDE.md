# 딱 걸렸어! — 프론트엔드 연동 가이드

React 개발자를 위한 Spring Boot API 연동 안내서입니다.

---

## 서버 실행 순서

프론트 개발 전 아래 두 서버가 모두 켜져 있어야 합니다.

```
1. FastAPI 분석 서버 (Python) → http://localhost:8000
2. Spring Boot 백엔드 (Java)  → http://localhost:8080
```

React에서는 **Spring Boot(8080)** 하고만 통신하면 됩니다.
FastAPI는 Spring Boot가 내부적으로 알아서 호출합니다.

---

## CORS 허용 포트

Spring Boot에서 아래 포트를 허용해두었습니다.

- `http://localhost:3000` (CRA)
- `http://localhost:5173` (Vite)

---

## 공통 사항

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json` (이미지 분석은 `multipart/form-data`)
- **인증**: 현재 없음 (로그인 기능 구현 후 추가 예정)

### 에러 응답 형식

모든 에러는 아래 형식으로 내려옵니다.

```json
{ "error": "에러 메시지" }
```

---

## API 목록

### 1. 서버 상태 확인

```
GET /health
```

**응답**

```json
{
  "status": "ok",
  "service": "딱 걸렸어! Spring Boot 서버",
  "python_server": "ok"
}
```

`python_server`가 `"unreachable"`이면 분석 서버가 꺼진 상태입니다.

---

### 2. 텍스트 분석

```
POST /analyze/text
Content-Type: application/json
```

**요청**

```json
{
  "content": "이 크림은 아토피를 치료합니다. 부작용이 전혀 없습니다."
}
```

**응답**

```json
{
  "original_text": "이 크림은 아토피를 치료합니다. 부작용이 전혀 없습니다.",
  "overall_suspicion_level": "의심",
  "overall_score": 0.91,
  "sentence_results": [
    {
      "sentence": "이 크림은 아토피를 치료합니다.",
      "suspicion_level": "의심",
      "matched_keywords": ["아토피", "치료"],
      "reason": "의약품으로 오인될 수 있는 표현이 포함되어 있습니다."
    },
    {
      "sentence": "부작용이 전혀 없습니다.",
      "suspicion_level": "의심",
      "matched_keywords": ["부작용"],
      "reason": "안전성을 단정하는 표현은 허위·과장 광고에 해당할 수 있습니다."
    }
  ],
  "summary": "총 2개 문장 중 2개에서 허위·과장 가능성이 높은 표현이 감지되었습니다."
}
```

**React 예시 (axios)**

```js
const analyzeText = async (content) => {
  const res = await axios.post('http://localhost:8080/analyze/text', { content });
  return res.data;
};
```

**React 예시 (fetch)**

```js
const analyzeText = async (content) => {
  const res = await fetch('http://localhost:8080/analyze/text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
};
```

---

### 3. URL 분석

```
POST /analyze/url
Content-Type: application/json
```

**요청**

```json
{
  "content": "https://example.com/product"
}
```

**응답** — 텍스트 분석과 동일한 형식

**React 예시**

```js
const analyzeUrl = async (url) => {
  const res = await axios.post('http://localhost:8080/analyze/url', { content: url });
  return res.data;
};
```

---

### 4. 이미지 분석

```
POST /analyze/image
Content-Type: multipart/form-data
```

**요청** — `file` 키로 이미지 파일 전송 (최대 10MB)

**응답** — 텍스트 분석과 동일한 형식 (`original_text`는 OCR 추출 텍스트)

**React 예시**

```js
const analyzeImage = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const res = await axios.post('http://localhost:8080/analyze/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data;
};
```

---

### 5. 내 분석 이력 (로그인 후 사용 가능)

> ⚠️ 로그인 기능 구현 중입니다. 현재는 임시로 `userId` 쿼리파라미터를 사용합니다.

```
GET /history?userId={userId}&page=0&size=10
```

**응답** — 페이징된 분석 결과 목록 (최신순)

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

### 6. 분석 이력 단건 조회

```
GET /history/{id}
```

---

## 응답 필드 정리

### `overall_suspicion_level` / `suspicion_level` 값

| 값 | 의미 | 권장 UI |
|---|---|---|
| `"정상"` | 허위·과장 표현 없음 | 초록색 |
| `"주의"` | 과장 가능성 있음 | 노란색 |
| `"의심"` | 허위·과장 의심 | 빨간색 |

### `overall_score`

- `0.0 ~ 1.0` 범위의 소수
- 숫자가 높을수록 의심도가 높음
- 퍼센트로 표시할 경우: `Math.round(overall_score * 100) + '%'`

---

## HTTP 상태코드

| 코드 | 상황 |
|---|---|
| `200` | 성공 |
| `400` | 요청값 오류 (content 없음, 파일 없음 등) |
| `500` | 분석 서버 통신 오류 또는 내부 오류 |
