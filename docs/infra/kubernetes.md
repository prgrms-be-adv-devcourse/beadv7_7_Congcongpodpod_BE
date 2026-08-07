# LastDish Kubernetes 매니페스트

## 디렉터리

```text
k8s/
├── 00-cluster/
│   └── namespaces.yaml
├── ingress-nginx/
│   └── values.yaml
├── cert-manager/
│   └── clusterissuers.yaml
├── platform/
│   └── config-server.yaml
├── data/
│   ├── redis.yaml
│   └── kafka.yaml
└── app/
    ├── configmaps/
    │   ├── database-config.yaml
    │   ├── gateway-jwt-public-key.yaml
    │   ├── redis-client-config.yaml
    │   └── kafka-client-config.yaml
    ├── services/
    │   ├── gateway-service.yaml
    │   ├── member-service.yaml
    │   └── core-service.yaml
    └── networking/
        ├── certificate.yaml
        └── ingress.yaml
```

- `00-cluster`: 네임스페이스처럼 클러스터 전체에 먼저 적용할 리소스
- `ingress-nginx`: ingress-nginx Helm 설정
- `cert-manager`: 인증서 발급 컨트롤러가 사용하는 클러스터 전역 발급자
- `platform`: Config Server 등 애플리케이션 공통 플랫폼
- `app`: Gateway와 비즈니스 애플리케이션

`ClusterIssuer`는 `cert-manager` 네임스페이스 소속이 아니라 클러스터 전역 리소스다. 관리 목적상 `cert-manager/`에 둔다.

Secret 값은 저장소에 커밋하지 않는다. 배포 전에 클러스터에서 직접 생성한다.

운영 서버의 `ldm` CLI 백업, 복원, Metrics Server 설치와 검증 절차는 [LastDish 운영 CLI와 Metrics Server](./lastdish-operations.md)를 참고한다.

## 적용 순서

```bash
kubectl apply -f 00-cluster/namespaces.yaml

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --values ingress-nginx/values.yaml

helm upgrade --install cert-manager \
  oci://quay.io/jetstack/charts/cert-manager \
  --version v1.20.1 \
  --namespace cert-manager \
  --set crds.enabled=true

kubectl apply -f cert-manager/clusterissuers.yaml
kubectl apply -f platform/config-server.yaml
kubectl apply -f data/redis.yaml
kubectl apply -f data/kafka.yaml
kubectl apply -f app/configmaps/
kubectl apply -f app/services/
kubectl apply -f app/networking/certificate.yaml
kubectl apply -f app/networking/ingress.yaml
```

Config Server와 GHCR Secret은 각 Deployment를 적용하기 전에 생성해야 한다. 전체 설치 절차는 Notion의 `인프라 구성 방법` 문서를 참고한다.

## 외부 PostgreSQL 연결

PostgreSQL은 Kubernetes 밖의 DB 전용 EC2 `10.30.2.93`에서 Docker Compose로 실행한다.

| 데이터베이스 | JDBC 주소 |
| --- | --- |
| Member | `jdbc:postgresql://10.30.2.93:5432/member_db` |
| Core | `jdbc:postgresql://10.30.2.93:5433/core_db` |

DB 서버 실행 절차는 [`infra/database/README.md`](../../infra/database/README.md)를 참고한다.

비밀번호를 터미널 기록에 남기지 않고 애플리케이션용 Secret을 생성한다. 사용자명과 비밀번호는 DB 서버의 `infra/database/.env`와 일치해야 한다.

```bash
read -s -p "Member DB password: " MEMBER_DB_PASSWORD
echo
read -s -p "Core DB password: " CORE_DB_PASSWORD
echo

kubectl create secret generic member-db-credentials \
  --namespace=app \
  --from-literal=username=member \
  --from-literal=password="$MEMBER_DB_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic core-db-credentials \
  --namespace=app \
  --from-literal=username=core \
  --from-literal=password="$CORE_DB_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

unset MEMBER_DB_PASSWORD CORE_DB_PASSWORD
```

서비스를 적용하고 외부 DB 연결 상태를 확인한다.

```bash
kubectl apply -f app/services/member-service.yaml
kubectl apply -f app/services/core-service.yaml
kubectl rollout status deployment/member-service -n app --timeout=180s
kubectl rollout status deployment/core-service -n app --timeout=180s

kubectl get pod,service -n app
```

```bash
curl -fsS https://api.lastdish.kr/actuator/health
```

## 애플리케이션 자동 배포

`main`에 서비스 코드가 push되면 해당 서비스 워크플로가 테스트 → `:dev` 이미지 GHCR push → EC2 SSH 접속 → 해당 Deployment 재시작을 수행한다. 다른 서비스는 재시작하지 않는다.

| 변경 경로 | 재시작 대상 |
| --- | --- |
| `backend/services/config-server/**` | `platform/config-server` |
| `backend/services/gateway-service/**` | `app/gateway-service` |
| `backend/services/member-service/**` | `app/member-service` |
| `backend/services/core-service/**` | `app/core-service` |
| `backend/modules/**`, 공통 Gradle 파일 | 영향받는 네 서비스 워크플로 모두 |

GitHub 저장소 Actions secrets가 필요하다.

- `EC2_HOST`: EC2 접속 호스트명
- `EC2_SSH_PRIVATE_KEY`: 배포 전용 SSH 개인 키 전체 내용
- `EC2_SSH_KNOWN_HOSTS`: `ssh-keyscan`으로 검증해 등록한 호스트 키

개인 키와 실제 Secret 값은 이 저장소에 커밋하지 않는다.
