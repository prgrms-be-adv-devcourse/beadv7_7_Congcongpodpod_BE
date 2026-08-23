# AI Service

## 책임

AI Service는 판매자가 등록한 음식 이미지를 분석해 상품 카테고리와 신뢰도 기반 추천 후보를 반환합니다.

```text
POST /api/v1/ai/classify
Content-Type: multipart/form-data
```

## 처리 흐름

```mermaid
flowchart LR
    Caller[내부 호출자 · 로컬 direct] --> AI[AI Service]
    AI --> RateLimit[요청 제한]
    RateLimit --> Engine[AI Engine]
    AI --> Search[(Elasticsearch)]
    AI --> S3[(S3)]
```

- `AiController`: multipart 요청과 응답 경계
- `AiService`: 분류 유스케이스
- `FastApiClient`: 외부 AI Engine 호출
- `RateLimitInterceptor`: 분류 요청 제한
- `FoodClassificationResponse`: category, confidence와 추천 후보 응답

## 의존성

- PostgreSQL `ai_db`
- Redis와 Bucket4j
- Kafka, Outbox, Inbox
- Elasticsearch
- S3
- `AI_ENGINE_URL`로 지정한 분류 엔진

## OpenAI 설정

AI Service가 임베딩 또는 OpenAI API를 직접 호출할 때 아래 환경변수를 사용합니다.

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `OPENAI_API_KEY` | 없음 | OpenAI API 키. 로컬 `.env` 또는 배포 Secret으로만 주입합니다. |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | 임베딩 모델 이름 |
| `OPENAI_CHAT_MODEL` | `gpt-4.1-mini` | 대화·구조화 응답 모델 이름 |

현재 이미지 분류는 `AI_ENGINE_URL`의 FastAPI 엔진을 사용하므로 OpenAI 키가 없어도 실행됩니다. OpenAI 연동 구현을 추가하면 시작 시 키 존재 여부를 검증하고, 키를 로그나 오류 응답에 포함하지 않아야 합니다.

현재 Gateway에는 AI OpenAPI 집계 경로만 있고 `/api/v1/ai/**` 비즈니스 라우트는 선언되어 있지 않습니다. 외부 공개 전 Gateway route와 인증 정책을 함께 추가해야 합니다.

## 실행과 검증

```bash
./dev/dev.sh ai-service

cd backend
./gradlew :services:ai-service:test
```

로컬 포트는 `8084`, 컨테이너 내부 포트는 `8080`입니다.
