# LastDish 배포 인프라

`infra/`는 EC2 서버에 배포하는 운영 인프라만 관리합니다. 로컬 개발용 Compose, 환경변수 예시, Config Server 설정, JWT 키 생성기와 Kibana는 [`dev/`](../dev/README.md)에서 관리합니다.

## 디렉터리 구조

```text
infra/
├── ec2-main/                        # Public Main EC2의 Kubernetes 기준 파일
│   ├── bootstrap/                   # Namespace 최초 생성
│   ├── namespaces/
│   │   ├── app/                     # Spring 서비스 ConfigMap·Workload
│   │   ├── platform/                # Config Server
│   │   ├── data/                    # Redis·Kafka
│   │   └── monitoring/              # Prometheus·Grafana·Alloy·Exporter
│   ├── cert-manager/                # ClusterIssuer
│   ├── ingress-nginx/               # Ingress Controller values
│   ├── networking/                  # TLS Certificate·Ingress
│   └── tools/                       # 서버 운영 도구·빌드 산출물(Git 제외)
└── ec2-data/                        # Private Data EC2의 Docker Compose 기준 파일
    ├── compose.yaml                 # PostgreSQL·Elasticsearch·Exporter·Loki·Alloy
    ├── .env.example                 # 운영자가 설정할 환경변수 이름
    ├── elasticsearch/               # Elasticsearch JVM 설정
    ├── loki/                        # Loki 설정
    └── alloy/                       # Docker 컨테이너 로그 수집 설정
```

## 서버별 책임

| 구분 | Main EC2 | Data EC2 |
| --- | --- | --- |
| 접속 별칭 | `ssh lastdish` | `ssh lastdish-data` |
| 실행 방식 | Kubernetes | Docker Compose |
| 애플리케이션 | Gateway, Config, Member, Core, Payment, AI | 없음 |
| 데이터 인프라 | Redis, Kafka | Member/Core PostgreSQL, Elasticsearch |
| 관측 | Prometheus, Grafana, kube-state-metrics, Node Exporter, Alloy | PostgreSQL/Elasticsearch/Node Exporter, Loki, Alloy, cAdvisor |
| 기준 경로 | `/home/ec2-user/k8s` | `/home/ec2-user/database` |

## 설정 관리 원칙

- `infra/ec2-main`과 `infra/ec2-data`가 배포 설정의 원본입니다. 서버에서 직접 수정한 내용은 저장소에도 반드시 반영합니다.
- 비밀번호·토큰·개인키는 Git에 커밋하지 않습니다.
- Kubernetes의 일반 설정은 ConfigMap, 민감정보는 Secret으로 주입합니다.
- Data EC2의 민감정보는 Git에서 제외된 `/home/ec2-user/database/.env`로 주입합니다.
- `.env.example`에는 실제 값이 아닌 필요한 환경변수 이름과 안전한 예시만 기록합니다.
- 운영 데이터 삭제, 볼륨 제거, Secret 교체는 적용 대상을 확인한 뒤 수행합니다.

## Main EC2 적용

Namespace와 공통 인프라를 먼저 적용한 뒤 서비스 Workload와 Networking을 적용합니다.

```bash
ssh lastdish
cd /home/ec2-user/k8s

kubectl apply -f bootstrap/namespaces.yaml
kubectl apply --recursive -f namespaces
kubectl apply --recursive -f networking
```

cert-manager와 ingress-nginx는 Helm 설치·업데이트 시 각 디렉터리의 기준 파일을 사용합니다. Secret은 저장소에 YAML 값으로 작성하지 않고 `kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -` 방식으로 관리합니다.

적용 후 확인:

```bash
kubectl get pods -A
kubectl get svc -A
kubectl get ingress -A
kubectl get certificate -A
```

## Data EC2 적용

```bash
ssh lastdish-data
cd /home/ec2-user/database

cp .env.example .env  # 최초 1회만 실행하고 실제 운영 값을 입력
chmod 600 .env

docker compose --env-file .env config --quiet
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

상세한 환경변수, 데이터 디렉터리, 상태 점검과 이미지 갱신 절차는 [`ec2-data/README.md`](ec2-data/README.md)를 따릅니다.

## 로컬 개발 환경

로컬 통합 환경은 `infra/`가 아니라 `dev/`에서 실행합니다.

```bash
cp dev/.env.example dev/.env
./dev/dev.sh
```

직접 Docker Compose를 사용할 때는 두 파일을 모두 명시합니다.

```bash
docker compose \
  --env-file dev/.env \
  --file dev/compose.yaml \
  up -d --build
```

세부 명령과 Windows 사용법은 [`dev/README.md`](../dev/README.md), Config·JWT·Kibana 구조는 [`dev/local/README.md`](../dev/local/README.md)를 참고합니다.
