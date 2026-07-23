# event-common

서비스 간 이벤트 계약과 이벤트 발행 포트를 제공하는 공통 모듈입니다.

## 제공 기능

- `DomainEvent<P>`: 이벤트 ID, 타입, Aggregate, 버전, 발생 시각 및 Payload 계약
- `EventMessage`: Outbox에서 읽은 직렬화 이벤트 전달 형식
- `EventPublisher`: 발행 기술과 도메인을 분리하는 포트
- `SpringEventPublisher`: Spring `ApplicationEventPublisher` 기반 구현
- `EventCommonAutoConfiguration`: Spring 발행 구현 자동 등록

## 사용

```gradle
implementation project(':modules:event-common')
```

기본 발행 구현은 Spring Event입니다.

```yaml
event:
  publisher: ${EVENT_PUBLISHER:spring}
```

`event.publisher`가 없거나 `spring`이면 `SpringEventPublisher`가 등록됩니다.
향후 Kafka 구현을 추가하더라도 `DomainEvent`, `EventMessage`, `EventPublisher` 계약은 유지합니다.

## META-INF 자동 구성 파일

경로:

```text
src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

등록 내용:

```text
kr.lastdish.common.event.EventCommonAutoConfiguration
```

Spring Boot는 의존 JAR의 이 파일을 읽어 `EventCommonAutoConfiguration`을 자동으로 적용합니다.
따라서 사용하는 서비스가 공통 모듈 패키지를 직접 컴포넌트 스캔하지 않아도
설정 조건에 맞는 `SpringEventPublisher`가 Spring Bean으로 등록됩니다.

