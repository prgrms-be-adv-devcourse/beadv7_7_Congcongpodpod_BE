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
