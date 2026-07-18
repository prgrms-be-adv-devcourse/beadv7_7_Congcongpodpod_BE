# 로컬 통합 환경

Config Server, Gateway, Member, Core와 서비스별 PostgreSQL을 Docker Compose로 함께 실행한다. 운영 Config 저장소나 PAT를 사용하지 않는다.

| 소유 서비스 | Compose 호스트 | 로컬 포트 | 데이터베이스 | 사용자 |
|---|---|---:|---|---|
| Member | `member-db:5432` | `localhost:54321` | `member_db` | `member` |
| Core | `core-db:5432` | `localhost:54322` | `core_db` | `core` |

두 데이터베이스는 컨테이너, 계정, 볼륨을 분리한다. 각 서비스는 자신의 데이터베이스에만 접근한다.

## 실행

저장소 루트에서:

```bash
./infra/local/generate-jwt-keys.sh
docker compose up --build -d
docker compose ps
```

키 생성 스크립트는 로컬 전용 Access/Refresh RSA 키 쌍을 `infra/local/keys`에 생성한다. 생성된 PEM 파일은 Git에 포함되지 않는다. 기존 키를 의도적으로 교체할 때만 `--force`를 사용한다.

```bash
./infra/local/generate-jwt-keys.sh --force
```

Windows PowerShell에서는 다음 명령을 사용한다. `openssl`이 PATH에 등록되어 있어야 한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\infra\local\generate-jwt-keys.ps1
```

Windows에서 기존 키를 교체하는 경우:

```powershell
powershell -ExecutionPolicy Bypass -File .\infra\local\generate-jwt-keys.ps1 -Force
```

Gateway는 Access Token 검증에 `access-public-key.pem`만 사용한다. IDE에서 Gateway를 직접 실행할 때는 저장소 루트 기준 공개키 경로를 전달한다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
JWT_PUBLIC_KEY_LOCATION=file:../infra/local/keys/access-public-key.pem \
  ./gradlew :services:gateway-service:bootRun
```

Windows PowerShell에서 직접 실행하는 경우:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "local"
$env:JWT_PUBLIC_KEY_LOCATION = "file:../infra/local/keys/access-public-key.pem"
.\gradlew.bat :services:gateway-service:bootRun
```

Member와 Core 애플리케이션을 Compose 밖에서 개별 실행하려면 DB와 Config Server만 먼저
실행한다.

```bash
docker compose up -d member-db core-db config-server
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

로컬 Config의 기본 URL이 각각 `localhost:54321`, `localhost:54322`를 사용하므로 DB 연결
환경변수는 필요 없다. `local` 프로필을 활성화해야 Swagger UI와 OpenAPI 문서가 제공된다.

## 확인

```bash
curl -fsS http://localhost:8888/member-service/default
curl -fsS http://localhost:8080/actuator/health
```

직접 호출:

```bash
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8082/actuator/health
```

## Swagger

통합 Swagger UI는 Gateway에서 제공한다.

```text
http://localhost:8080/swagger-ui/index.html
```

화면 상단에서 `member-service` 또는 `core-service` 문서를 선택한다. 각 문서의 Servers
목록에서 Gateway 통합 호출과 서비스 직접 호출을 전환할 수 있다.

| 문서 | Gateway | Direct |
|---|---|---|
| Member | `http://localhost:8080` | `http://localhost:8081` |
| Core | `http://localhost:8080` | `http://localhost:8082` |

보호 API는 Swagger UI의 `Authorize`에 Access Token을 입력한 뒤 호출한다. 서비스별 UI도
각각 `http://localhost:8081/swagger-ui/index.html`,
`http://localhost:8082/swagger-ui/index.html`에서 사용할 수 있다. Swagger와 Direct 호출용
CORS는 `local` 프로필에서만 활성화된다.

데이터베이스 확인:

```bash
docker compose exec member-db psql -U member -d member_db -c 'select current_database(), current_user;'
docker compose exec core-db psql -U core -d core_db -c 'select current_database(), current_user;'
```

## 로그와 종료

```bash
docker compose logs -f gateway-service member-service core-service
docker compose down
```

이미지까지 삭제하려면:

```bash
docker compose down --rmi local
```

로컬 DB 데이터까지 초기화하려면 다음 명령을 사용한다. 모든 로컬 데이터가 삭제된다.

```bash
docker compose down -v
```

현재 DB 비밀번호는 로컬 개발 전용 공개 값이다. 운영 비밀번호, 토큰, Private key는 저장소에 추가하지 않는다. `infra/local/keys`의 키는 로컬 환경에서만 사용한다.
