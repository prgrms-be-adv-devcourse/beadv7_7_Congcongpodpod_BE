# LastDish Backend

Java 21, Spring Boot 4.1.0, Spring Cloud 2025.1.2 기반 Gradle 멀티 프로젝트입니다.

## 로컬 전체 환경 실행

저장소 루트에서 Config Server, Gateway, Member, Core를 한 번에 실행합니다.

```bash
docker compose up --build -d
docker compose ps
```

```bash
curl -fsS http://localhost:8080/api/members/hello
curl -fsS http://localhost:8080/api/core/hello
```

포트는 Gateway `8080`, Member `8081`, Core `8082`, Config Server `8888`입니다. 로컬 Config는 `infra/local/config`을 사용하며 운영 Config 저장소와 인증정보가 필요하지 않습니다.

```bash
docker compose logs -f
docker compose down
```

## 로컬에서 서비스 단위로 빌드하기

아래 명령은 모두 `backend` 디렉터리에서 실행합니다.

```bash
cd backend
```

서비스별 테스트:

```bash
./gradlew :services:config-server:test
./gradlew :services:gateway-service:test
./gradlew :services:member-service:test
./gradlew :services:core-service:test
```

서비스별 실행 JAR 생성:

```bash
./gradlew :services:config-server:bootJar
./gradlew :services:gateway-service:bootJar
./gradlew :services:member-service:bootJar
./gradlew :services:core-service:bootJar
```

`:`로 구분한 `:services:<서비스명>:<작업>` 형식은 `settings.gradle`에 등록된 Gradle 멀티 프로젝트 경로입니다. 예를 들어 `:services:core-service:bootJar`는 Core Service의 실행 JAR만 생성합니다. 필요한 공통 프로젝트가 있다면 Gradle이 해당 의존성도 함께 빌드합니다.

서비스별 Docker 이미지 생성:

```bash
docker build --platform linux/amd64 -f services/config-server/Dockerfile -t lastdish-config-server:local .
docker build --platform linux/amd64 -f services/gateway-service/Dockerfile -t lastdish-gateway-service:local .
docker build --platform linux/amd64 -f services/member-service/Dockerfile -t lastdish-member-service:local .
docker build --platform linux/amd64 -f services/core-service/Dockerfile -t lastdish-core-service:local .
```

`linux/amd64`는 현재 AWS EC2 인스턴스 아키텍처에 맞추기 위한 옵션입니다.

## GitHub Actions 이미지 빌드 범위

- `services/config-server/**` 변경: Config Server 이미지만 빌드
- `services/gateway-service/**` 변경: Gateway Service 이미지만 빌드
- `services/member-service/**` 변경: Member Service 이미지만 빌드
- `services/core-service/**` 변경: Core Service 이미지만 빌드
- 루트 Gradle 설정, Gradle Wrapper 또는 공통 모듈 변경: 영향받는 서비스 워크플로가 모두 빌드
- 각 이미지 워크플로 파일 변경: 해당 서비스 이미지만 빌드

`main` 브랜치에 푸시하면 실행된 워크플로는 GHCR의 `dev` 태그를 갱신하고, 추적 가능한 `sha-<커밋>` 태그도 함께 게시합니다. 문서만 변경하면 이미지 빌드는 실행되지 않습니다.
