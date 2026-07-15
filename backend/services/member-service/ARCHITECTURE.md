# Member Service 아키텍처

`member-service`는 인증과 회원 기능을 함께 제공하는 모듈러 모놀리식 서비스다. 하나의 애플리케이션으로 빌드하고 배포하지만, `auth`와 `member`의 업무 경계를 분리한다.

## 모듈 구성

```text
kr.lastdish.member
├── auth
├── member
├── shared
└── support
```

- `auth`: 로그인, 로그아웃, 토큰 발급·갱신 및 인증 처리를 담당한다.
- `member`: 회원가입, 회원 정보, 주소 및 회원 상태를 관리한다.
- `shared`: 특정 업무 모듈에 속하지 않는 최소한의 공통 타입과 기술 기능을 관리한다.
- `support`: 운영 지원 기능이나 업무 모듈에 포함되지 않는 임시 기능을 관리한다.

## 모듈 내부 구조

각 업무 모듈은 실제 기능을 추가할 때 다음 구조를 따른다.

```text
<module>
├── domain
├── application
├── infrastructure
└── presentation
```

## 의존성 규칙

1. `member` 모듈은 회원 정보와 회원 상태를 소유한다.
2. `auth` 모듈은 인증 수단과 토큰 발급 정책을 소유한다.
3. 모듈 간 동기 호출은 명시적으로 공개된 `application` API를 사용한다.
4. 다른 모듈의 `domain`, `infrastructure`, `presentation` 패키지를 직접 참조하지 않는다.
5. 즉시 결과가 필요하지 않은 후속 처리는 Spring Application Event를 사용한다.
6. `shared`에는 인증·회원 비즈니스 규칙을 두지 않는다.
7. 별도 서비스인 `core-service`와는 Kubernetes Service 기반 HTTP 통신을 사용한다.
