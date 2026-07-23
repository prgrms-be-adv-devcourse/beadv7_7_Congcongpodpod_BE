# Core Service 아키텍처

`core-service`는 모듈러 모놀리식으로 구성한다. 애플리케이션 하나로 빌드하고 배포하지만, 각 업무 기능은 독립적인 경계를 가진 Bounded Context로 관리한다.

## 모듈 구성

```text
kr.lastdish.core
├── cart
├── dish
├── order
├── payment
├── settlement
├── store
├── common
└── support
```

- `cart`: 사용자의 장바구니와 장바구니 항목을 관리한다.
- `dish`: 메뉴 정보와 판매 가능 상태를 관리한다.
- `order`: 주문 생성과 주문 상태를 관리한다.
- `payment`: 결제와 예치금 상태를 관리한다.
- `settlement`: 정산 기능의 업무 경계다.
- `store`: 매장 정보와 영업 상태를 관리한다.
- `common`: Core Service 내부에서만 사용하는 공통 타입을 관리한다.
- `support`: Swagger 등 애플리케이션 지원 설정을 관리한다.

## 모듈 내부 구조

각 업무 모듈은 실제 기능을 추가할 때 다음 구조를 따른다.

```text
<module>
├── domain
├── application
├── infrastructure
└── presentation
```

- `domain`: 도메인 모델과 비즈니스 규칙
- `application`: 유스케이스, 트랜잭션 및 다른 모듈에 공개하는 API
- `infrastructure`: 데이터베이스와 외부 시스템 연동 구현
- `presentation`: HTTP 요청과 응답을 처리하는 어댑터

## 의존성 규칙

1. 각 모듈은 자신의 도메인 모델과 데이터베이스 테이블을 소유한다.
2. 다른 모듈은 해당 모듈의 `domain`, `infrastructure`, `presentation` 패키지를 직접 참조하지 않는다.
3. 즉시 결과가 필요한 모듈 간 동기 호출은 명시적으로 공개된 `application` API를 사용한다.
4. 즉시 결과가 필요하지 않은 후속 처리는 Spring Application Event를 사용한다.
5. `common`에는 도메인 비즈니스 규칙을 두지 않으며, 모듈 간 결합을 우회하는 용도로 사용하지 않는다.
6. 별도 서비스와 통신이 필요하면 Spring Application Event가 아닌 명시적인 외부 통신 계약을 사용한다.
