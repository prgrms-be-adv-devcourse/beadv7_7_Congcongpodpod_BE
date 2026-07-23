# api-common

서비스 간 API 응답과 예외 계약을 통일하는 공통 모듈입니다.

## 제공 기능

- `ApiResponse`: 성공·실패 응답 형식
- `ErrorCodeSpec`: 서비스별 오류 코드가 구현할 공통 계약
- `CommonErrorCode`: 입력 오류, 리소스 없음, 상태 오류 등 공통 오류 코드
- `BusinessException`: `ErrorCodeSpec`과 메시지를 전달하는 비즈니스 예외

서비스별 도메인 오류 코드는 각 서비스에서 `ErrorCodeSpec`을 구현해 관리합니다.

```java
public enum MemberErrorCode implements ErrorCodeSpec {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "회원을 찾을 수 없습니다.");
}
```

```java
throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
```

## 사용

```gradle
implementation project(':modules:api-common')
```

이 모듈은 Spring Bean을 등록하지 않는 순수 계약 모듈이므로
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`가 없습니다.

