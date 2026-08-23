# LastDish Kubernetes 배포

운영 인프라의 원본은 `infra/`입니다. Main EC2는 Kubernetes로 애플리케이션을 실행하고, Data EC2와 Log 서버는 Docker Compose로 데이터·관측 인프라를 실행합니다.

## 구조

```text
infra/
├── ec2-main/
│   ├── bootstrap/          # Namespace 최초 생성
│   ├── namespaces/
│   │   ├── app/            # Gateway, Member, Core, Payment, AI
│   │   ├── platform/       # Config Server
│   │   ├── data/           # Redis, Kafka
│   │   └── monitoring/     # Alloy, exporters
│   ├── cert-manager/
│   ├── ingress-nginx/
│   ├── networking/
│   └── apply-secrets.sh
├── ec2-data/               # PostgreSQL, Elasticsearch, exporters
└── ec2-log/                # Loki, Prometheus, Grafana
```

전체 서버별 책임과 적용 명령은 [`infra/README.md`](../../infra/README.md)를 기준으로 합니다.

## Main EC2 적용 순서

1. `bootstrap/namespaces.yaml`로 namespace를 생성합니다.
2. ingress-nginx와 cert-manager를 설치합니다.
3. Config Server, Redis, Kafka와 monitoring 리소스를 적용합니다.
4. GitHub Container Registry pull secret과 애플리케이션 secret을 생성합니다.
5. Gateway, Member, Core, Payment, AI Deployment를 적용합니다.
6. Certificate와 Ingress를 적용합니다.

```bash
kubectl apply -f infra/ec2-main/bootstrap/namespaces.yaml
kubectl apply --recursive -f infra/ec2-main/namespaces
kubectl apply --recursive -f infra/ec2-main/networking

kubectl get pods -A
kubectl get ingress,certificate -A
```

Secret은 저장소 YAML에 값을 작성하지 않고 `infra/ec2-main/apply-secrets.sh`와 Git에서 제외된 환경 파일로 반영합니다. 자세한 계약은 [`namespaces/app/README.md`](../../infra/ec2-main/namespaces/app/README.md)를 확인하세요.

## Data EC2

Data EC2는 PostgreSQL 데이터베이스, Elasticsearch와 exporter를 실행합니다.

```bash
cd infra/ec2-data
cp .env.example .env
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d
```

환경변수, 데이터 디렉터리, 순차 시작과 점검 절차는 [`ec2-data/README.md`](../../infra/ec2-data/README.md)를 따릅니다.

## 자동 배포

백엔드 서비스 워크플로는 변경 서비스를 테스트하고 GHCR 이미지를 만든 뒤 해당 Deployment만 재시작합니다.

| 변경 경로 | 이미지·Deployment |
| --- | --- |
| `backend/services/config-server/**` | Config Server |
| `backend/services/gateway-service/**` | Gateway Service |
| `backend/services/member-service/**` | Member Service |
| `backend/services/core-service/**` | Core Service |
| `backend/services/payment-service/**` | Payment Service |
| `backend/services/ai-service/**` | AI Service |
| `backend/modules/**`, 공통 Gradle 설정 | 영향을 받는 서비스 |

배포는 rollout 실패 시 undo하고 복구 상태를 다시 확인합니다. 배포에 필요한 SSH와 registry 자격 증명은 GitHub Actions secrets로 관리합니다.

## 운영 검증

```bash
kubectl get deployments,pods,services --all-namespaces
kubectl rollout status deployment/gateway-service --namespace=app --timeout=180s
curl -fsS https://api.lastdish.kr/actuator/health
```

로그·메트릭·백업과 장애 대응은 [운영 가이드](lastdish-operations.md)를 확인하세요.
