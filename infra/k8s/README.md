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
│   └── postgresql.yaml
└── app/
    ├── certificate.yaml
    ├── ingress.yaml
    ├── gateway-service.yaml
    ├── member-service.yaml
    └── core-service.yaml
```

- `00-cluster`: 네임스페이스처럼 클러스터 전체에 먼저 적용할 리소스
- `ingress-nginx`: ingress-nginx Helm 설정
- `cert-manager`: 인증서 발급 컨트롤러가 사용하는 클러스터 전역 발급자
- `platform`: Config Server 등 애플리케이션 공통 플랫폼
- `data`: Member/Core 전용 PostgreSQL과 영속 볼륨
- `app`: Gateway와 비즈니스 애플리케이션

`ClusterIssuer`는 `cert-manager` 네임스페이스 소속이 아니라 클러스터 전역 리소스다. 관리 목적상 `cert-manager/`에 둔다.

Secret 값은 저장소에 커밋하지 않는다. 배포 전에 클러스터에서 직접 생성한다.

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
kubectl apply -f data/postgresql.yaml
kubectl apply -f app/certificate.yaml
kubectl apply -f app/gateway-service.yaml
kubectl apply -f app/member-service.yaml
kubectl apply -f app/core-service.yaml
kubectl apply -f app/ingress.yaml
```

Config Server와 GHCR Secret은 각 Deployment를 적용하기 전에 생성해야 한다. 전체 설치 절차는 Notion의 `인프라 구성 방법` 문서를 참고한다.

## PostgreSQL 배포 준비

현재 단일 노드 이름 `team03-k8s`와 EC2 로컬 디스크를 사용한다. 클라우드나 노드 이름이 바뀌면 `data/postgresql.yaml`의 `nodeAffinity`와 저장 경로를 함께 수정해야 한다.

EC2에서 데이터 디렉터리를 먼저 생성한다.

```bash
sudo mkdir -p \
  /var/lib/lastdish/postgresql/member \
  /var/lib/lastdish/postgresql/core

sudo chown -R 999:999 /var/lib/lastdish/postgresql
sudo chmod 700 \
  /var/lib/lastdish/postgresql/member \
  /var/lib/lastdish/postgresql/core
```

비밀번호를 터미널 기록에 남기지 않고 Secret을 생성한다. Secret은 네임스페이스를 넘어서 참조할 수 없으므로 DB가 있는 `data`와 애플리케이션이 있는 `app`에 각각 생성한다.

```bash
read -s -p "Member DB password: " MEMBER_DB_PASSWORD
echo
read -s -p "Core DB password: " CORE_DB_PASSWORD
echo

for namespace in data app; do
  kubectl create secret generic member-db-credentials \
    --namespace="$namespace" \
    --from-literal=database=member_db \
    --from-literal=username=member \
    --from-literal=password="$MEMBER_DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -

  kubectl create secret generic core-db-credentials \
    --namespace="$namespace" \
    --from-literal=database=core_db \
    --from-literal=username=core \
    --from-literal=password="$CORE_DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
done

unset MEMBER_DB_PASSWORD CORE_DB_PASSWORD
```

DB와 서비스를 순서대로 적용하고 확인한다.

```bash
kubectl apply -f data/postgresql.yaml
kubectl rollout status statefulset/member-db -n data --timeout=180s
kubectl rollout status statefulset/core-db -n data --timeout=180s

kubectl apply -f app/member-service.yaml
kubectl apply -f app/core-service.yaml
kubectl rollout status deployment/member-service -n app --timeout=180s
kubectl rollout status deployment/core-service -n app --timeout=180s

kubectl get pod,service,pvc -n data
kubectl get pv
```

DB 연결을 확인한다.

```bash
kubectl exec -n data statefulset/member-db -- \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'

kubectl exec -n data statefulset/core-db -- \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'

curl -fsS https://api.lastdish.kr/actuator/health
```

`PersistentVolume`의 reclaim policy는 `Retain`이다. StatefulSet이나 PVC를 삭제해도 EC2 디스크 데이터는 자동 삭제되지 않는다. DB 디렉터리를 직접 삭제하기 전에는 반드시 백업한다.

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
