# ADCheck Spring Boot API 명세서

Base URL: `http://localhost:8080`
응답 포맷: JSON(UTF-8)
인증 방식: JWT (Authorization: Bearer <token> 또는 token 쿠키)

---
## 공통 에러
```json
{ "error": "에러 메시지" }
```

---
## 헬스체크
- `GET /health`
  - 서비스 및 Python 연동 상태 확인

---
## 사용자

### 회원가입 (이메일 인증 완료 필수)
- `POST /register`
- Body
```json
{ "email": "user@example.com", "password": "secret123", "nickname": "닉네임" }
```
- 200: `{ "message": "회원가입이 완료되었습니다." }`
- 400: `{ "error": "이메일 인증을 완료한 뒤 회원가입할 수 있습니다." }`
- 409: `{ "error": "이미 사용 중인 이메일입니다." }`

### 로그인
- `POST /login`
- Body
```json
{ "email": "user@example.com", "password": "secret123" }
```
- 200: 쿠키(`token`, httpOnly, secure) 설정 + 바디
```json
{ "token": "<jwt>", "nickname": "닉네임" }
```
- 401: `{ "error": "이메일 또는 비밀번호가 일치하지 않습니다." }`

### 비밀번호 변경 (인증 필요)
- `POST /password/change`
- Headers: `Authorization: Bearer <jwt>` 또는 token 쿠키
- Body
```json
{ "currentPassword": "oldPass", "newPassword": "newPass123" }
```
- 200: `{ "message": "비밀번호가 변경되었습니다." }`
- 400/401: 에러 메시지 반환

### 이메일 인증 코드 발송
- `POST /email/send-code`
- Body
```json
{ "email": "user@example.com" }
```
- 200: `{ "message": "인증 코드가 이메일로 전송되었습니다.", "email": "user@example.com" }`
- 400: `{ "error": "이미 가입된 이메일입니다." }`

### 이메일 인증 코드 검증
- `POST /email/verify-code`
- Body
```json
{ "email": "user@example.com", "code": "123456" }
```
- 200: `{ "message": "이메일 인증이 완료되었습니다." }`
- 400: `{ "error": "인증 코드가 일치하지 않습니다." }` 등

---
## 콘텐츠 분석
### 텍스트 분석
- `POST /analyze/text`
- Body: `{ "content": "분석할 텍스트" }`
- 200: 분석 결과 JSON 반환
- 400: `{ "error": "content가 비어있습니다." }`

### URL 분석
- `POST /analyze/url`
- Body: `{ "content": "https://example.com/article" }`
- 200: 분석 결과 JSON 반환 (Python 서버가 URL 내용을 가져와 분석)
- 400: `{ "error": "content가 비어있습니다." }`

### 이미지 분석
- `POST /analyze/image`
- multipart/form-data
  - `file`: 이미지(jpg, png, ≤10MB)
- 200: 분석 결과 JSON 반환
- 400: `{ "error": "파일이 비어있습니다." }`
  - Python 서버 통신 실패 시 500 에러 응답

---
## 히스토리 (인증 필요)
### 목록 조회
- `GET /history?page=0&size=10`
- 200: 분석 내역 페이지 리스트

### 상세 조회
- `GET /history/{id}`
- 200: 단일 분석 내역

---
## 메일(SMTP) 설정 가이드
- 환경변수: `SMTP_HOST`(기본 smtp.gmail.com), `SMTP_PORT`(기본 587), `SMTP_USERNAME`, `SMTP_PASSWORD`
- 타임아웃: `SMTP_CONNECTION_TIMEOUT` / `SMTP_TIMEOUT` / `SMTP_WRITETIMEOUT` (기본 5000ms)
- TLS: `spring.mail.properties.mail.smtp.starttls.enable=true`, `SMTP_SSL_PROTOCOLS` 기본 `TLSv1.2`
- Gmail 사용 시 앱 비밀번호 필요(보안 수준이 낮은 앱 허용 대신).
- 네이버 메일 예시: `SMTP_HOST=smtp.naver.com`, `SMTP_PORT=587`, `SMTP_USERNAME=아이디@naver.com`, `SMTP_PASSWORD=네이버 앱비밀번호`, `SMTP_SSL_TRUST=smtp.naver.com`
- 메일 제목: `[ADCheck] 이메일 인증 코드`, 내용에 6자리 코드와 10분 유효 시간 안내
