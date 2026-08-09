# LastDish 인프라

실행 위치와 서버 역할을 기준으로 관리합니다.

```text
infra/
├── local/                           # Docker Compose 로컬 개발 보조 파일
│   ├── config-server/config/        # 공통·서비스별 Config Server 설정
│   ├── member-service/              # JWT 키 생성 스크립트와 서명 키
│   ├── gateway-service/keys/        # Gateway JWT 검증 공개키
│   ├── kibana/                      # 로컬 Kibana와 Data EC2 SSH 터널 실행 파일
├── ec2-main/                        # Main EC2 Kubernetes 기준 매니페스트
│   ├── bootstrap/                   # Namespace 최초 생성
│   ├── namespaces/
│   │   ├── app/                     # Spring 서비스 ConfigMap·Deployment·Service
│   │   ├── platform/                # Config Server
│   │   ├── data/                    # Redis·Kafka
│   │   └── monitoring/              # Prometheus·Grafana·Alloy·Exporter
│   ├── cert-manager/                # ClusterIssuer
│   ├── ingress-nginx/               # Ingress Controller values
│   ├── networking/                  # TLS Certificate·Ingress
│   └── tools/                       # 서버 운영 도구
└── ec2-data/                        # Data EC2 Docker Compose
    ├── compose.yaml                 # PostgreSQL·Elasticsearch·Exporter·Loki·Alloy
    ├── .env.example                 # 필요한 환경변수 이름
    ├── elasticsearch/               # Elasticsearch JVM 설정
    ├── loki/                        # Loki 설정
    └── alloy/                       # 컨테이너 로그 수집 설정
```

## 설정 기준

- 로컬 전체 서비스는 저장소 루트 `compose.yaml`로 실행합니다.
- Config Server는 `infra/local/config-server/config`를 읽기 전용으로 마운트합니다.
- `application.yml`은 공통 설정, `<service>.yml`은 서비스 기본 설정, `<service>-local.yml`은 로컬 override입니다.
- Main EC2의 기준 파일은 `ec2-main`, Data EC2의 기준 파일은 `ec2-data`입니다. 서버에서 직접 수정한 내용은 저장소에도 반드시 반영합니다.
- 실제 비밀번호·토큰·개인키는 Git에 커밋하지 않습니다. Kubernetes Secret 또는 Git에서 제외된 `.env`로 주입합니다.

## 주요 진입점

```bash
# 로컬 전체 빌드·실행과 교체된 이전 이미지 정리
./dev.sh

# 선택한 로컬 DB·메시지·캐시·검색 데이터를 초기화
./dev.sh reset member-db

# 모든 로컬 데이터 저장소를 초기화하고 전체 환경 재생성
./dev.sh reset all

# 로컬 JWT 키 생성
./infra/local/member-service/generate-jwt-keys.sh

# 로컬 Kibana 실행
./infra/local/kibana/start-kibana.sh

# Data EC2 Compose 실행
docker compose --env-file .env up -d
```

Kubernetes는 Namespace → ConfigMap/RBAC/스토리지 → Workload → Networking 순서로 적용합니다. 세부 운영 절차와 접속 방법은 팀 Notion 문서를 기준으로 합니다.
