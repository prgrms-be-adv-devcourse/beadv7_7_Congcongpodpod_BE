# Payment Service

## 책임

Payment Service는 결제 관련 이벤트와 결제 처리 데이터를 독립된 경계에서 관리하기 위한 서비스입니다. 현재 외부 결제 API는 Core Service의 `/api/v1/payments/**` 경로에 있으며, Payment Service는 Kafka·Outbox·Inbox 기반 비동기 결제 흐름을 위한 실행 기반을 제공합니다.

## 의존성

- PostgreSQL `payment_db`
- Redis
- Kafka
- Toss Payments 설정
- Config Server

로컬에서는 Core PostgreSQL 컨테이너에 별도 데이터베이스와 계정을 생성하지만 Core 스키마를 직접 조회하지 않습니다.

## 이벤트 안전성

- 발행할 이벤트는 Outbox에 기록합니다.
- 소비한 이벤트는 Inbox에서 event ID와 aggregate version으로 중복·순서를 확인합니다.
- Kafka topic 구성은 `PaymentKafkaTopicConfig`에서 관리합니다.

## 실행과 검증

```bash
./dev/dev.sh payment-service

cd backend
./gradlew :services:payment-service:test
```

로컬 포트는 `8083`, 컨테이너 내부 포트는 `8080`입니다.
