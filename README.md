# LastDish

Flutter 프론트엔드와 Java 21, Spring Boot 4.1.0, Spring Cloud 2025.1.2 기반
멀티 서비스 백엔드로 구성됩니다.
Gateway를 단일 진입점으로 사용하며 Member/Core Service와 서비스별 PostgreSQL을
분리합니다.

## 서비스 구성

| 서비스 | 로컬 포트 | 역할 |
|---|---:|---|
| Gateway | `8080` | 라우팅, JWT 검증, 역할 기반 접근 제어 |
| Member | `8081` | 인증과 회원 |
| Core | `8082` | 가게, 상품, 장바구니, 주문, 결제, 정산, 예치금 |
| Config Server | `8888` | 서비스 설정 제공 |

상세 문서는 [`docs`](docs/README.md)에서 확인합니다.

## 디렉터리 구성

- `frontend/`: Flutter 애플리케이션
- `backend/`: Spring Boot 멀티 서비스
- `infra/`: 로컬·Kubernetes 인프라 설정
- `docs/`: 아키텍처와 서비스 문서

## 로컬 실행

저장소 루트에서 실행합니다.

```bash
./infra/local/generate-jwt-keys.sh
docker compose up -d --build
docker compose ps
```

```bash
curl -fsS http://localhost:8080/actuator/health
```

통합 Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

화면 상단에서 `member-service` 또는 `core-service` 문서를 선택합니다. 개별 API의
요청·응답 DTO와 세부 명세는 Swagger를 기준으로 확인합니다.

Gateway 라우팅, 접근 허용 목록, 내부 인증 헤더와 오류 코드는
[`docs/backend/gateway.md`](docs/backend/gateway.md)를 참고합니다.

```bash
docker compose logs -f gateway-service member-service core-service
docker compose down
```

세미프로젝트 프론트엔드는 Flutter Web을 기본 대상으로 사용합니다.

Flutter를 처음 사용하는 경우 운영체제에 맞게 SDK를 설치합니다. Windows 설치와
환경 변수 설정은 [`frontend/README.md`](frontend/README.md)를 참고합니다.

```bash
# macOS
brew install --cask flutter
flutter doctor
flutter config --enable-web
```

```bash
cd frontend
flutter pub get
flutter run -d chrome --web-port 3000
```

API 주소 설정과 웹 배포 빌드는 [`frontend/README.md`](frontend/README.md)를
참고합니다.
