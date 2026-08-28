# Gateway Service

## 책임

Gateway Service는 외부 요청의 단일 진입점입니다. Spring Cloud Gateway WebFlux 기반으로 라우팅, JWT 검증, 역할 기반 접근 제어, CORS, 요청 ID 전파와 통합 OpenAPI를 담당합니다.

## 라우팅

| 대상 | 경로 |
| --- | --- |
| Member Service | `/api/v1/auth/**`, `/api/v1/members/**`, `/api/v1/notifications/**` |
| Core Service | `/api/v1/carts/**`, `/orders/**`, `/stores/**`, `/dishes/**`, `/payments/**`, `/settlements/**`, `/deposits/**`, `/levels/**`, `/points/**`, `/favorites/**`, `/locations/**` |
| OpenAPI | `/openapi/{member-service|core-service|payment-service|ai-service}` |

실제 경로 원본은 `dev/local/config-server/gateway-service.yml`입니다.

## 보안 경계

Gateway는 JWT 서명, 만료, issuer와 역할을 검증한 뒤 외부에서 전달된 내부 인증 헤더를 제거하고 다음 값을 직접 생성합니다.

- `X-Authenticated-Member-Id`
- `X-Authenticated-Role`

하위 서비스는 외부 JWT가 아니라 이 헤더 계약을 사용합니다. 공개 경로와 역할별 허용 목록은 [Gateway 상세 문서](../backend/gateway.md)를 확인하세요.

## 운영 계약

- 로컬 주소: `http://localhost:8080`
- 통합 Swagger: `http://localhost:8080/swagger-ui/index.html`
- 연결 제한: 3초
- 응답 제한: 10초
- 관측: health, info, gateway, refresh, prometheus

## 검증

```bash
cd backend
./gradlew :services:gateway-service:test
```
