# mvc-common

Spring MVC 서비스의 예외 응답 처리를 통일하는 공통 모듈입니다.

## 제공 기능

- `GlobalExceptionHandler`: `BusinessException`, 요청 검증 오류, 잘못된 요청 형식 및 예상하지 못한 예외 처리
- `MvcCommonAutoConfiguration`: Servlet 기반 웹 애플리케이션에 `GlobalExceptionHandler` 등록

예외 응답은 `api-common`의 `ApiResponse`와 `ErrorCodeSpec`을 사용합니다.

## 사용

```gradle
implementation project(':modules:api-common')
implementation project(':modules:mvc-common')
```

별도의 `@ComponentScan`이나 `@Import(GlobalExceptionHandler.class)`는 필요하지 않습니다.

## META-INF 자동 구성 파일

경로:

```text
src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

파일에는 다음 자동 구성 클래스가 등록되어 있습니다.

```text
kr.lastdish.common.mvc.MvcCommonAutoConfiguration
```

Spring Boot는 애플리케이션 시작 시 의존 JAR의 이 파일을 읽고 자동 구성 클래스를 불러옵니다.
`MvcCommonAutoConfiguration`은 Servlet 웹 애플리케이션에서만 활성화되며
`GlobalExceptionHandler`를 Spring Bean으로 등록합니다.

