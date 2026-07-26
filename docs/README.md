# LastDish 문서

프로젝트의 개발·운영 문서를 한곳에서 관리합니다.

## Architecture

- [전체 시스템 아키텍처](architecture.md)

## Backend

- [Gateway 라우팅, 인증, 오류 응답](backend/gateway.md)
- [Swagger 사용 가이드](backend/swagger.md)
- [Member Service 로컬 실행](backend/member-local-run.md)
- [빌드, Spotless, CI, Docker 이미지](backend/build-and-ci.md)

## Services

- [Member Service 구조](services/member-service.md)
- [Core Service 구조](services/core-service.md)

## 공통 모듈

- [api-common](modules/api-common.md): 공통 API 응답과 예외 계약
- [event-common](modules/event-common.md): 서비스 간 이벤트 계약
- [mvc-common](modules/mvc-common.md): Spring MVC 공통 예외 처리
- [outbox](modules/outbox.md): Transactional Outbox 지원

## 인프라

- [로컬 통합 개발 환경](infra/local-development.md)
- [Kubernetes 매니페스트](infra/kubernetes.md)

## 문서 작성 원칙

- 루트 `README.md`에는 프로젝트 개요와 빠른 시작만 작성합니다.
- 상세 설명은 주제별로 `docs` 아래에 작성합니다.
- 공통 모듈 문서는 `docs/modules`, 환경·배포 문서는 `docs/infra`에 추가합니다.
- 코드나 설정 경로가 바뀌면 관련 문서 링크와 예시도 함께 수정합니다.
