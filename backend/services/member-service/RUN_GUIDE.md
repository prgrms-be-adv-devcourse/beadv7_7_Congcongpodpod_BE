# Member Service 로컬 실행 가이드

## 사전 요구사항

- Java 21
- Docker 및 Docker Compose
- OpenSSL

명령은 별도 안내가 없다면 저장소 루트에서 실행합니다.

## 1. 로컬 JWT 키 생성

최초 실행 전에 Access/Refresh RSA 키를 생성합니다.

```bash
./infra/local/generate-jwt-keys.sh
```

키는 `infra/local/keys`에 생성되며 Git에 포함하지 않습니다. 기존 키를 의도적으로 교체할 때만 다음 명령을 사용합니다.

```bash
./infra/local/generate-jwt-keys.sh --force
```

Windows PowerShell에서는 저장소 루트에서 다음 명령을 실행합니다. `openssl`이 `PATH`에 등록되어 있어야 합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\infra\local\generate-jwt-keys.ps1
```

기존 키를 교체할 때만 `-Force`를 사용합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\infra\local\generate-jwt-keys.ps1 -Force
```

## 2. 의존 서비스 실행

Member DB와 Config Server만 실행합니다.

```bash
docker compose up -d member-db config-server
docker compose ps
```

Config Server가 `healthy`가 된 후 Member Service를 실행합니다.

## 3. Member Service 단독 실행

`backend` 디렉터리에서 Gradle `bootRun`을 실행합니다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local SERVER_PORT=8081 \
  ./gradlew :services:member-service:bootRun
```

Windows PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "8081"
.\gradlew.bat :services:member-service:bootRun
```

Gradle이 애플리케이션 작업 디렉터리를 `backend/services/member-service`로 설정하므로, `application-local.yml`의 기본 JWT 키 경로는 해당 디렉터리를 기준으로 `../../../infra/local/keys`를 참조합니다.

다른 위치의 키를 사용할 경우 환경변수로 경로를 지정합니다.

```bash
SPRING_PROFILES_ACTIVE=local \
SERVER_PORT=8081 \
JWT_ACCESS_PRIVATE_KEY_LOCATION=file:/absolute/path/access-private-key.pem \
JWT_ACCESS_PUBLIC_KEY_LOCATION=file:/absolute/path/access-public-key.pem \
  ./gradlew :services:member-service:bootRun
```

## 4. 실행 확인

```bash
curl -fsS http://localhost:8081/actuator/health
```

정상 응답:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

로컬 Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

## 5. 테스트 및 빌드

테스트는 Config Server를 사용하지 않고 H2와 테스트 전용 RSA 키로 실행됩니다.

```bash
cd backend
./gradlew :services:member-service:test
./gradlew spotlessCheck build
```

## 6. Gateway를 포함한 전체 실행

저장소 루트에서 실행합니다.

```bash
docker compose up --build -d
docker compose ps
```

Windows PowerShell에서도 저장소 루트에서 같은 Docker Compose 명령을 사용합니다.

```powershell
docker compose up --build -d
docker compose ps
```

Compose는 Member Service에 Access 개인키와 공개키를 `/run/secrets`로 마운트합니다. Gateway는 동일한 Access 공개키로 토큰 서명을 검증합니다.

Gateway의 기본 주소는 `http://localhost:8080`입니다. 회원가입과 로그인은 Member Service의 8081 포트가 아니라 Gateway를 통해 요청합니다.

### macOS/Linux 회원가입 및 로그인

회원가입:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "userName": "localmember",
    "password": "securePassword123!",
    "name": "로컬회원",
    "phone": "010-1234-5678",
    "email": "localmember@example.com",
    "role": "MEMBER"
  }'
```

로그인:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "localmember@example.com",
    "password": "securePassword123!"
  }'
```

### Windows PowerShell 회원가입 및 로그인

회원가입:

```powershell
$signUpBody = @{
  userName = "localmember"
  password = "securePassword123!"
  name = "로컬회원"
  phone = "010-1234-5678"
  email = "localmember@example.com"
  role = "MEMBER"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/signup" `
  -ContentType "application/json" `
  -Body $signUpBody
```

로그인:

```powershell
$loginBody = @{
  email = "localmember@example.com"
  password = "securePassword123!"
} | ConvertTo-Json

$tokens = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$tokens.accessToken
$tokens.refreshToken
```

정상 로그인 응답에는 `accessToken`과 `refreshToken`이 포함되어야 합니다.

## 7. 현재 통합 검증 결과

- Gateway Health: `200 UP`
- Gateway 경유 회원가입: `200`, 회원 저장 성공
- Gateway 경유 로그인: `500`, JWT 미발급

로그인 실패 원인은 `AuthService.login()`에서 Refresh Token JWT 문자열을 `BCryptPasswordEncoder`로 인코딩하는 코드입니다. Refresh Token이 BCrypt 입력 한도인 72바이트를 초과하여 다음 예외가 발생합니다.

```text
java.lang.IllegalArgumentException: password cannot be more than 72 bytes
```

Refresh Token 저장용 해시는 비밀번호용 BCrypt와 분리해야 합니다. 예를 들어 SHA-256 해시 또는 Refresh Token의 별도 식별자(`jti`)를 저장하고 재발급 시 비교하는 방식으로 수정한 뒤 로그인 성공과 Gateway의 Access Token 검증을 다시 확인해야 합니다.

## 문제 해결

### `RSA 키페어 초기화 실패`

- `infra/local/keys/access-private-key.pem`과 `access-public-key.pem`이 존재하는지 확인합니다.
- Gradle 명령을 `backend`에서 실행했는지 확인합니다.
- IntelliJ에서 직접 실행한다면 Working directory와 JWT 키 환경변수를 확인합니다.

### `relation "members" does not exist`

서비스는 기동됐지만 로컬 DB 스키마가 준비되지 않은 상태입니다. 현재 로컬 설정은 Hibernate가 테이블을 자동 생성하지 않으므로, 프로젝트에서 사용하는 DB 스키마 또는 마이그레이션을 먼저 적용해야 합니다.

### 로그인 시 `password cannot be more than 72 bytes`

Refresh Token 전체를 BCrypt로 인코딩하여 발생합니다. JWT는 BCrypt의 입력 길이 제한을 초과할 수 있으므로 Refresh Token 전용 해시 전략으로 변경해야 합니다.

### 종료

터미널의 Member Service는 `Ctrl+C`로 종료합니다. 로컬 인프라는 저장소 루트에서 종료합니다.

```bash
docker compose stop member-db config-server
```
