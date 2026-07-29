# LastDish 문서

루트 [README](../README.md)는 프로젝트 개요와 빠른 시작을, 이 디렉터리는 개발·운영
상세 내용을 다룹니다.

## 처음 보는 경우

1. [전체 시스템 아키텍처](architecture.md)
2. [로컬 통합 환경](infra/local-development.md)
3. [Swagger 사용 가이드](backend/swagger.md)

## 개발 문서

### Frontend

- [Flutter 설치, 실행, 웹 빌드](../frontend/README.md)

### Backend

- [Gateway 라우팅, 인증, 오류 응답](backend/gateway.md)
- [Swagger 사용 가이드](backend/swagger.md)
- [Member Service 로컬 실행](backend/member-local-run.md)
- [빌드, Spotless, CI, Docker 이미지](backend/build-and-ci.md)

### 서비스

- [Member Service 구조](services/member-service.md)
- [Core Service 구조](services/core-service.md)

### 공통 모듈

- [api-common](modules/api-common.md): 공통 API 응답과 예외 계약
- [event-common](modules/event-common.md): 서비스 간 이벤트 계약
- [mvc-common](modules/mvc-common.md): Spring MVC 공통 예외 처리
- [outbox](modules/outbox.md): Transactional Outbox 지원

## 인프라·운영 문서

- [로컬 통합 환경](infra/local-development.md): 루트 `compose.yaml` 기반 전체 백엔드 실행
- [Kubernetes 매니페스트](infra/kubernetes.md): 배포 구성과 적용 순서

## 문서 작성 원칙

- 루트 `README.md`에는 프로젝트 개요와 빠른 시작만 작성합니다.
- 주제별 상세 내용은 `docs/`에 작성하고 루트 README에서 연결합니다.
- 공통 모듈은 `docs/modules/`, 환경·배포는 `docs/infra/`에 작성합니다.
- 코드나 설정 경로가 바뀌면 관련 링크와 명령도 함께 검증합니다.
