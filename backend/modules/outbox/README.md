# outbox

도메인 변경과 이벤트 기록을 같은 DB 트랜잭션에 저장하고,
저장된 이벤트를 비동기로 발행하는 Transactional Outbox 공통 모듈입니다.

## 처리 흐름

```text
도메인 트랜잭션
→ OutboxEventWriter.append()
→ outbox_events에 PENDING 저장
→ OutboxScheduler가 이벤트 선점
→ OutboxEventProcessor가 EventPublisher로 발행
→ 성공 시 PUBLISHED
→ 실패 시 재시도 후 FAILED
```

## 사용

```gradle
implementation project(':modules:event-common')
implementation project(':modules:outbox')
```

도메인 변경 트랜잭션 안에서 이벤트를 기록합니다.

```java
outboxEventWriter.append(domainEvent);
```

설정:

```yaml
event:
  publisher: ${EVENT_PUBLISHER:spring}

outbox:
  scheduler:
    enabled: ${OUTBOX_SCHEDULER_ENABLED:true}
  polling-delay-ms: ${OUTBOX_POLLING_DELAY_MS:1000}
  batch-size: ${OUTBOX_BATCH_SIZE:100}
  max-retries: ${OUTBOX_MAX_RETRIES:5}
  lock-timeout-seconds: ${OUTBOX_LOCK_TIMEOUT_SECONDS:60}
```

서비스별 DB에 `outbox_events` 테이블이 필요합니다. 운영 환경에서는 Hibernate 자동 생성보다
DB 마이그레이션으로 테이블을 관리해야 합니다.

## META-INF 자동 구성 파일

경로:

```text
src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

등록 내용:

```text
kr.lastdish.common.outbox.OutboxAutoConfiguration
```

Spring Boot는 의존 JAR의 이 파일을 읽어 `OutboxAutoConfiguration`을 자동 적용합니다.
이 파일이 없으면 모듈이 의존성에 있어도 서비스의 기본 컴포넌트 스캔 범위 밖에 있는
Outbox 구성과 Bean이 자동 등록되지 않습니다.

## OutboxAutoConfiguration

Outbox 모듈의 진입점인 Spring Boot 자동 구성 클래스입니다.

- `@AutoConfiguration(before = ...)`: Hibernate와 Spring Data JPA 자동 구성보다 먼저 실행
- `@EnableScheduling`: `@Scheduled` 기반 Outbox 발행 활성화
- `@Import`: Writer, Processor, Scheduler, Repository Adapter, Serializer 등 Outbox Bean 등록
- `OutboxAutoConfigurationPackagesRegistrar`도 함께 불러 JPA 스캔 패키지 등록

서비스가 `kr.lastdish.common.outbox` 패키지를 직접 스캔하지 않아도
Outbox 애플리케이션·인프라 구성 요소를 사용할 수 있게 합니다.

## OutboxAutoConfigurationPackagesRegistrar

다음 패키지를 Spring Boot의 자동 구성 기준 패키지에 추가합니다.

```text
kr.lastdish.common.outbox
```

Spring Boot의 JPA 자동 구성은 자동 구성 기준 패키지를 사용해 Entity와 Repository를 찾습니다.
Outbox 모듈은 Core 또는 Member 서비스의 기본 패키지 밖에 있으므로 별도 등록이 없으면
`OutboxEvent`와 `OutboxJpaRepository`가 검색되지 않습니다.

Registrar는 `AutoConfigurationPackages.register(...)`로 Outbox 패키지를 기존 서비스 패키지에
추가합니다. 따라서 서비스 자체의 Entity와 Repository 스캔 범위를 덮어쓰지 않으면서
Outbox JPA 구성도 같은 `EntityManagerFactory`에 합류시킵니다.

이 클래스는 자동 구성 내부 구현이므로 package-private으로 유지하며,
서비스 코드에서 직접 호출하거나 `@Import`하지 않습니다.

