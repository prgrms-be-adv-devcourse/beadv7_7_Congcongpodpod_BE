# LastDish Swagger 사용 가이드

## 1. 목적

Swagger UI를 사용해 Member, Core, Payment, AI Service의 API 명세를 확인하고 브라우저에서 직접
API를 호출한다. Swagger는 수동 API 확인 도구이며 자동화 테스트를 대체하지 않는다.

## 2. 현재 제공 범위

Gateway 통합 Swagger UI와 서비스별 Swagger UI를 모두 제공한다.

| 서비스 | Swagger UI | OpenAPI JSON |
|---|---|---|
| Gateway 통합 | `http://localhost:8080/swagger-ui/index.html` | 서비스별 문서를 Gateway가 프록시 |
| Member | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` |
| Core | `http://localhost:8082/swagger-ui/index.html` | `http://localhost:8082/v3/api-docs` |
| Payment | `http://localhost:8083/swagger-ui/index.html` | `http://localhost:8083/v3/api-docs` |
| AI | `http://localhost:8084/swagger-ui/index.html` | `http://localhost:8084/v3/api-docs` |

Swagger는 `local` 프로필에서만 활성화된다. 기본 프로필과 운영 환경에서는 Swagger UI와
OpenAPI 문서를 제공하지 않는다.

## 3. Docker Compose로 실행

저장소 루트에서 로컬 JWT 키를 생성하고 전체 서비스를 실행한다.

```bash
./dev/local/member-service/generate-jwt-keys.sh
docker compose --env-file dev/.env --file dev/compose.yaml up --build -d
docker compose --env-file dev/.env --file dev/compose.yaml ps
```

Compose는 Member `8081`, Core `8082`, Payment `8083`, AI `8084`를 노출하고 각 서비스의
`local` 프로필을 활성화한다.

실행 후 다음 주소에 접속한다.

```text
Gateway: http://localhost:8080/swagger-ui/index.html
Member: http://localhost:8081/swagger-ui/index.html
Core:   http://localhost:8082/swagger-ui/index.html
Payment: http://localhost:8083/swagger-ui/index.html
AI:      http://localhost:8084/swagger-ui/index.html
```

통합 문서 탐색은 Gateway UI를 사용하고, 서비스를 분리해 확인할 때는 각 서비스 UI를 사용한다. Gateway의 실제 비즈니스 라우트는 Member와 Core에만 선언되어 있으므로 Payment·AI 문서의 API는 Gateway 경유 호출 가능 여부를 별도로 확인한다.

## 4. 서비스 개별 실행

애플리케이션만 개별 실행하려면 DB와 Config Server를 먼저 실행한다.

```bash
docker compose --env-file dev/.env --file dev/compose.yaml up -d member-db core-db config-server
```

이후 저장소 루트에서 서비스별로 서로 다른 터미널을 사용한다.

```bash
# Terminal 1: Member Service
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 \
  ./backend/gradlew -p backend :services:member-service:bootRun

# Terminal 2: Core Service
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8082 \
  ./backend/gradlew -p backend :services:core-service:bootRun
```

Windows PowerShell에서는 다음과 같이 실행한다.

```powershell
# Terminal 1: Member Service
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "8081"
.\backend\gradlew.bat -p backend :services:member-service:bootRun

# Terminal 2: Core Service
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "8082"
.\backend\gradlew.bat -p backend :services:core-service:bootRun
```

`local` 프로필을 지정하지 않으면 Swagger가 비활성화된다. 두 서비스를 같은 기본 포트에서
동시에 실행할 수 없으므로 각각 `8081`, `8082`를 지정한다.

## 5. Swagger UI 사용 방법

1. 서비스의 Swagger UI 주소에 접속한다.
2. 테스트할 API를 펼친다.
3. `Try it out`을 선택한다.
4. 필요한 Path, Query, Request Body 값을 입력한다.
5. `Execute`를 선택하고 응답 상태와 본문을 확인한다.

문서의 `Servers` 메뉴에서 요청 대상을 선택할 수 있다.

| 문서 | Gateway 경유 | 서비스 직접 호출 |
|---|---|---|
| Member | `http://localhost:8080` | `http://localhost:8081` |
| Core | `http://localhost:8080` | `http://localhost:8082` |
| Payment | Gateway 비즈니스 route 미등록 | `http://localhost:8083` |
| AI | Gateway 비즈니스 route 미등록 | `http://localhost:8084` |

- Gateway 경유: Gateway 라우팅을 포함한 요청 흐름을 확인한다.
- 서비스 직접 호출: Gateway를 우회해 서비스 자체 동작을 확인한다.

## 6. JWT 인증 사용 방법

Swagger UI 오른쪽의 `Authorize`를 선택하고 Access Token 값만 입력한다.

```text
eyJhbGciOiJSUzI1NiJ9...
```

`Bearer ` 접두사는 Swagger UI가 자동으로 추가한다.

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

Gateway 경유 요청에서는 Gateway의 JWT 검증과 권한 정책이 적용된다. Direct 요청은
Gateway를 우회하므로 Gateway의 인증, 인가, 사용자 헤더 주입을 검증하지 않는다.

## 7. API URI 규격

외부 API는 `/api/v1/{resource}` 형식을 사용한다. Gateway는 경로를 변경하지 않고 각
서비스에 그대로 전달한다.

```text
/api/v1/auth/**, /api/v1/members/** -> member-service
/api/v1/carts/**, /api/v1/orders/**, /api/v1/stores/**,
/api/v1/dishes/**, /api/v1/payments/**, /api/v1/settlements/**,
/api/v1/deposits/**,  /api/v1/levels/**, /api/v1/points/**               -> core-service
```

권한은 URI에 `seller` 같은 역할명을 넣지 않고 Gateway에서 HTTP Method와 리소스 경로를
기준으로 검사한다.

## 8. Gateway 통합 UI

`http://localhost:8080/swagger-ui/index.html`에 접속하고 화면 상단의
`Select a definition`에서 문서를 선택한다.

- `member-service`: Member API 문서
- `core-service`: Core API 문서
- `payment-service`: Payment API 문서
- `ai-service`: AI API 문서

Gateway는 다음 경로를 각 서비스의 `/v3/api-docs`로 프록시한다.

```text
/openapi/member-service -> member-service:/v3/api-docs
/openapi/core-service   -> core-service:/v3/api-docs
/openapi/payment-service -> payment-service:/v3/api-docs
/openapi/ai-service      -> ai-service:/v3/api-docs
```

문서를 선택한 뒤 `Servers`에서 Gateway 또는 Direct 주소를 선택해 동일한 API를 통합·개별
방식으로 각각 확인할 수 있다.

## 9. 종료

```bash
docker compose --env-file dev/.env --file dev/compose.yaml down
```

DB 데이터까지 삭제해야 할 때만 다음 명령을 사용한다.

```bash
docker compose --env-file dev/.env --file dev/compose.yaml down -v
```

`down -v`는 Member, Core, Payment, AI 로컬 데이터베이스가 포함된 볼륨을 삭제하므로 주의한다.
