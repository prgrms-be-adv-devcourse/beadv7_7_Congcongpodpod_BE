# LastDish 문서

이 디렉터리는 LastDish의 설계, 개발, 로컬 실행, 배포와 운영 계약을 관리합니다. 프로젝트 소개와 최소 실행 절차는 [루트 README](../README.md)를 먼저 확인하세요.

## 문서 지도

| 목적 | 시작 문서 |
| --- | --- |
| 시스템 전체 이해 | [시스템 아키텍처](architecture.md) |
| 로컬에서 전체 백엔드 실행 | [로컬 통합 환경](infra/local-development.md) |
| 앱·웹 실행 | [Universal frontend](../frontend/react-native/README.md) |
| API 탐색과 인증 테스트 | [Swagger 가이드](backend/swagger.md) |
| 빌드·포맷·CI 확인 | [Backend 빌드와 CI](backend/build-and-ci.md) |
| Kubernetes 배포 | [Kubernetes 매니페스트](infra/kubernetes.md) |
| 운영 점검과 장애 대응 | [운영 가이드](infra/lastdish-operations.md) |

## 아키텍처와 공통 계약

- [시스템 아키텍처](architecture.md): 서비스 경계, 데이터 소유권, 요청·이벤트 흐름
- [Gateway](backend/gateway.md): 라우팅, JWT 검증, 역할 정책, 오류 매핑
- [Swagger](backend/swagger.md): Gateway 통합 OpenAPI와 서비스별 문서

## 서비스

| 서비스 | 책임 | 로컬 포트 | 문서 |
| --- | --- | ---: | --- |
| Gateway | 인증·인가, 라우팅, 통합 OpenAPI | `8080` | [gateway-service](services/gateway-service.md) |
| Member | 인증, 회원, 알림, SSE | `8081` | [member-service](services/member-service.md) |
| Core | 매장, 상품, 주문, 결제, 정산, 포인트 | `8082` | [core-service](services/core-service.md) |
| Payment | 결제 이벤트 처리와 결제 데이터 경계 | `8083` | [payment-service](services/payment-service.md) |
| AI | 상품 이미지 분류와 추천 후보 생성 | `8084` | [ai-service](services/ai-service.md) |
| Config Server | 환경별 Spring 설정 제공 | `8888` | [로컬 설정](../dev/local/README.md) |

컨테이너 내부 애플리케이션 포트는 `8080`이며 위 표는 `dev/compose.yaml`이 호스트에 공개하는 포트입니다.

## 공통 모듈

| 모듈 | 책임 | 문서 |
| --- | --- | --- |
| `api-common` | 공통 응답, 오류 계약, 요청 ID, 시간대 직렬화 | [api-common](modules/api-common.md) |
| `mvc-common` | MVC 예외 처리와 요청 로깅 | [mvc-common](modules/mvc-common.md) |
| `event-common` | 도메인 이벤트 계약과 Spring/Kafka 발행 | [event-common](modules/event-common.md) |
| `outbox` | 트랜잭션 Outbox 저장·선점·재시도 | [outbox](modules/outbox.md) |
| `inbox` | 소비 이벤트 멱등성·순서·실패 기록 | [inbox](modules/inbox.md) |
| `s3-storage` | Presigned URL과 업로드 메타데이터 | [s3-storage](modules/s3-storage.md) |

## 개발과 운영

### 개발

- [Member Service 단독 실행](backend/member-local-run.md)
- [Backend 빌드와 CI](backend/build-and-ci.md)
- [로컬 통합 환경](infra/local-development.md)
- [개발 도구 명령](../dev/README.md)

### 배포·운영

- [Kubernetes 매니페스트](infra/kubernetes.md)
- [운영 CLI와 Metrics Server](infra/lastdish-operations.md)

## 문서 변경 원칙

1. 코드와 설정이 문서보다 우선합니다. 포트·환경변수·라우트는 `dev/compose.yaml`, Config Server 설정과 각 서비스 코드를 기준으로 확인합니다.
2. 루트 README에는 제품 개요와 빠른 시작만 두고 상세 절차는 `docs/` 또는 컴포넌트별 README로 연결합니다.
3. 서비스 문서는 책임과 경계를, 모듈 문서는 재사용 계약과 자동 구성 조건을 설명합니다.
4. 명령은 저장소 루트 기준인지 하위 디렉터리 기준인지 명시합니다.
5. 문서 이동·추가 시 상대 링크 검사를 함께 수행합니다.
