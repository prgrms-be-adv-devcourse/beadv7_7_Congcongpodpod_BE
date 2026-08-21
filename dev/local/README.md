# 로컬 인프라

서비스 소유권이 드러나도록 로컬 설정과 JWT 키를 분리합니다.

```text
dev/local/
├── config-server/config/       # Native Config Server가 제공하는 설정
├── member-service/
│   ├── generate-jwt-keys.sh    # macOS·Linux 키 생성
│   ├── generate-jwt-keys.ps1   # Windows 키 생성
│   └── keys/                   # JWT 서명 개인키와 자체 검증 공개키(생성됨)
├── gateway-service/keys/       # Gateway 검증 공개키(생성됨)
└── kibana/                     # SSH 터널로 Data EC2 Elasticsearch를 조회하는 로컬 Kibana
```

키는 저장소 루트에서 생성합니다.

```bash
./dev/local/member-service/generate-jwt-keys.sh
```

생성된 PEM은 Git에서 제외됩니다. Member는 개인키로 JWT를 서명하고 공개키로 JWT를
검증합니다. Gateway에는 같은 공개키만 복사됩니다. Dockerfile에는 설정이나 키를 넣지
않으며, `dev/compose.yaml`이 Config와 PEM을 실행 시점에 읽기 전용으로 마운트합니다.

로컬 Kibana는 `kibana/.env`의 service account token을 사용해 다음 명령으로 실행합니다.

```bash
./dev/kibana/start-kibana.sh
```

상세 사용법은 팀 Notion 문서를 기준으로 합니다.

## Config Server 설정 배포

`config-server/config`의 아래 6개 파일은 로컬과 배포 환경이 함께 사용하는 원본입니다.
프로필별 `*-local.yml` 오버레이는 사용하지 않습니다.

```text
application.yml
member-service.yml
core-service.yml
payment-service.yml
ai-service.yml
gateway-service.yml
```

외부 Config Server 저장소에는 파일 내용을 수정하지 않고 그대로 복사합니다.

```bash
./dev/local/config-server/sync-config-server-config.sh /path/to/config-server-repository-root
```

환경 차이는 애플리케이션 YAML이 아니라 실행 환경의 ConfigMap·Secret·Compose 환경변수로
주입합니다. Kafka가 이벤트 발행의 기본값이며, 특별한 장애 격리 목적이 아니면
`EVENT_PUBLISHER`를 설정하지 않습니다.

배포 환경 필수 변수:

- 서비스 부트스트랩: `CONFIG_SERVER_URL`, 환경에 맞는 `SPRING_PROFILES_ACTIVE`
- 업무 서비스 공통: `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `REDIS_PORT`,
  `REDIS_USERNAME`, `REDIS_PASSWORD` (Gateway에는 Kafka 불필요)
- Member: `MEMBER_DB_URL`, `MEMBER_DB_USERNAME`, `MEMBER_DB_PASSWORD`,
  `JWT_ACCESS_PRIVATE_KEY_LOCATION`, `JWT_ACCESS_PUBLIC_KEY_LOCATION`,
  `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `KAKAO_ADMIN_KEY`
- Core: `CORE_DB_URL`, `CORE_DB_USERNAME`, `CORE_DB_PASSWORD`, `MEMBER_SERVICE_BASE_URL`,
  `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`, `ENCRYPTION_SECRET_KEY`
- Payment: `PAYMENT_DB_URL`, `PAYMENT_DB_USERNAME`, `PAYMENT_DB_PASSWORD`,
  `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`
- AI: `AI_DB_URL`, `AI_DB_USERNAME`, `AI_DB_PASSWORD`, `ELASTICSEARCH_URL`,
  `ELASTICSEARCH_USERNAME`, `ELASTICSEARCH_PASSWORD`
- Gateway: `JWT_PUBLIC_KEY_LOCATION`, `CORE_SERVICE_URL`, `MEMBER_SERVICE_URL`,
  `PAYMENT_SERVICE_URL`, `AI_SERVICE_URL`, `GATEWAY_CORS_ORIGIN`, `GATEWAY_CORS_WWW_ORIGIN`

Swagger는 기본 비활성입니다. 로컬·시연 환경에서만 `SPRINGDOC_ENABLED=true`를 주입합니다.

Core의 S3 연동은 로컬과 배포 환경 모두 `S3_ENABLED`, `S3_BUCKET`, `AWS_REGION`,
`S3_PRESIGNED_URL_EXPIRATION`, `S3_MAX_UPLOAD_SIZE`를 사용합니다. 로컬에서 실제 AWS S3를
사용할 때는 `dev/.env`에 access key를 넣고 `S3_ENABLED=true`로 설정합니다. 배포 환경은
장기 access key 대신 인스턴스·워크로드 IAM Role을 사용합니다. `S3_ENDPOINT`는 AWS S3가
아닌 호환 저장소를 연결할 때만 설정합니다.

배포 환경의 콘솔 로그는 `TZ=Asia/Seoul`, `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`로
설정합니다. 그러면 `requestId`가 ECS JSON 필드로 기록되고, 타임스탬프는 서울 시간대
오프셋으로 출력됩니다. 로컬은 해당 변수를 설정하지 않아 사람이 읽는 기본 콘솔 형식을
유지합니다. 필요하면 `LOGGING_STRUCTURED_JSON_EXCLUDE`,
`LOGGING_STRUCTURED_STACKTRACE_MAX_LENGTH`, `SKIP_SUCCESSFUL_ACTUATOR_LOGS`로 조정합니다.
