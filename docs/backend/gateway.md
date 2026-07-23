# Gateway

외부 요청의 단일 진입점입니다. 요청 경로에 따라 Member/Core Service로 라우팅하고
Access Token 검증과 역할 기반 접근 제어를 수행합니다.

## 로컬 주소

| 항목 | URL |
|---|---|
| Gateway | `http://localhost:8080` |
| Health Check | `http://localhost:8080/actuator/health` |
| Swagger UI (`local` 프로필) | `http://localhost:8080/swagger-ui/index.html` |
| Member OpenAPI | `http://localhost:8080/openapi/member-service` |
| Core OpenAPI | `http://localhost:8080/openapi/core-service` |

개별 API의 요청·응답 DTO와 세부 명세는 Swagger UI를 기준으로 확인합니다.

## 라우팅 API

로컬 라우팅 설정은
[`infra/local/config/gateway-service.yml`](../../infra/local/config/gateway-service.yml)에
있습니다.

| 외부 경로 | 대상 서비스 | 주요 기능 |
|---|---|---|
| `/api/v1/auth/**` | Member | 회원가입, 로그인, 토큰 재발급, 로그아웃 |
| `/api/v1/members/**` | Member | 회원 |
| `/api/v1/carts/**` | Core | 장바구니 |
| `/api/v1/orders/**` | Core | 주문 |
| `/api/v1/stores/**` | Core | 가게 |
| `/api/v1/dishes/**` | Core | 상품 |
| `/api/v1/payments/**` | Core | 결제 |
| `/api/v1/settlements/**` | Core | 정산 |
| `/api/v1/deposits/**` | Core | 입금·예치금 |

일반 API 경로는 변경하지 않고 대상 서비스로 전달합니다. 통합 문서 경로만 다음과 같이
변환합니다.

| Gateway 요청 | 대상 서비스 요청 |
|---|---|
| `/openapi/member-service` | Member `/v3/api-docs` |
| `/openapi/core-service` | Core `/v3/api-docs` |

## 하위 서비스 타임아웃

Gateway HTTP Client에는 모든 하위 서비스 라우트에 적용되는 연결·응답 제한 시간이
설정되어 있습니다.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            connect-timeout: 3000
            response-timeout: 10s
```

| 설정 | 값 | 의미 |
|---|---:|---|
| `connect-timeout` | `3000ms` | 하위 서비스와 TCP 연결을 맺을 때까지 기다리는 최대 시간 |
| `response-timeout` | `10s` | 연결 후 하위 서비스 응답을 기다리는 최대 시간 |

연결 자체가 불가능하면 `G503`, 연결 후 응답 제한 시간을 초과하면 `G504`를 반환합니다.
운영 환경에서는 Config Server의 `gateway-service.yml`에도 같은 설정을 추가해야 합니다.

## 접근 허용 목록

보안 규칙은
[`GatewaySecurityConfig`](../../backend/services/gateway-service/src/main/java/kr/lastdish/gateway/security/GatewaySecurityConfig.java)에
선언하며 위에서 아래 순서로 처음 일치한 규칙을 적용합니다.

### 인증 없이 허용

| HTTP 메서드 | 경로 | 용도 |
|---|---|---|
| `POST` | `/api/v1/auth/signup` | 회원가입 |
| `POST` | `/api/v1/auth/login` | 로그인 |
| `POST` | `/api/v1/auth/refresh` | Access Token 재발급 |
| 모든 메서드 | `/actuator/health/**` | Health Check |
| 모든 메서드 | `/swagger-ui.html`, `/swagger-ui/**` | Swagger UI |
| 모든 메서드 | `/v3/api-docs/**`, `/openapi/**` | OpenAPI 문서 |
| `GET` | `/api/v1/stores/**` | 가게 공개 조회 |
| `GET` | `/api/v1/dishes/**` | 상품 공개 조회 |

### `SELLER` 역할 필요

| 경로 | 비고 |
|---|---|
| `/api/v1/stores/**` | 공개 `GET`을 제외한 가게 API |
| `/api/v1/dishes/**` | 공개 `GET`을 제외한 상품 API |
| `/api/v1/settlements/**` | 정산 API |

### `MEMBER` 또는 `SELLER` 역할 필요

| 경로 | 주요 기능 |
|---|---|
| `/api/v1/auth/logout` | 로그아웃 |
| `/api/v1/members/**` | 회원 |
| `/api/v1/carts/**` | 장바구니 |
| `/api/v1/orders/**` | 주문 |
| `/api/v1/payments/**` | 결제 |
| `/api/v1/deposits/**` | 입금·예치금 |

목록에 없는 경로는 기본적으로 거부합니다. 신규 API 추가 시 라우팅 설정과
`GatewaySecurityConfig`의 접근 규칙을 함께 수정합니다.

## 인증과 내부 헤더

보호 API는 Access Token을 전달해야 합니다.

```http
Authorization: Bearer <ACCESS_TOKEN>
```

Gateway는 JWT의 서명·만료·발급자를 검증합니다. 인증 성공 시 외부에서 전달된 동일
이름의 헤더를 제거하고 검증된 JWT로 내부 헤더를 다시 생성합니다.

| 내부 헤더 | JWT 값 | 용도 |
|---|---|---|
| `X-Authenticated-Member-Id` | `sub` | 인증 회원 식별자 |
| `X-Authenticated-Role` | `role` | `MEMBER` 또는 `SELLER` 역할 |

## 오류 응답

Gateway와 하위 서비스는 공통 `ApiResponse` 형식을 사용합니다.

```json
{
  "success": false,
  "error": {
    "code": "G001",
    "message": "인증이 필요합니다."
  },
  "timestamp": "2026-07-23T00:00:00Z"
}
```

Gateway가 직접 관리하는 오류:

| HTTP 상태 | 코드 | 의미 |
|---:|---|---|
| 401 | `G001` | 인증 정보 누락 또는 유효하지 않은 Access Token |
| 403 | `G002` | 인증됐지만 역할 권한 부족 |
| 400 | `G003` | Gateway가 처리할 수 없는 잘못된 요청 |
| 404 | `G004` | Gateway 라우트 없음 |
| 500 | `G500` | 분류되지 않은 Gateway 내부 오류 |
| 502 | `G502` | 하위 서비스 응답 처리 실패 |
| 503 | `G503` | 하위 서비스 연결·DNS·라우팅 실패 |
| 504 | `G504` | 하위 서비스 응답 시간 초과 |

하위 서비스가 반환한 `Cxxx` 또는 서비스별 도메인 오류 코드는 Gateway가 변경하지 않고
전달합니다.

## 검사

```bash
cd backend
./gradlew :services:gateway-service:spotlessCheck :services:gateway-service:test
```
