# Monitoring 네임스페이스

Prometheus가 Data EC2의 PostgreSQL·Elasticsearch exporter를 수집하고 Grafana가 Prometheus를 기본 데이터 소스로 조회한다. 둘 다 Kubernetes 내부에서 실행하며 Grafana는 초기 검증 단계에서 외부에 공개하지 않는다.

## 구성

| 구성 요소 | 이미지 | 메모리 상한 | 저장소 |
| --- | --- | ---: | ---: |
| Prometheus | `prom/prometheus:v3.13.1` | `384Mi` | `5Gi`, 7일 또는 4GB |
| Grafana | `grafana/grafana:13.1.0` | `256Mi` | `1Gi` |
| kube-state-metrics | `registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.19.1` | `128Mi` | - |

Prometheus target은 `10.30.2.93:9187`(Member PostgreSQL), `10.30.2.93:9188`(Core PostgreSQL), `10.30.2.93:9114`(Elasticsearch)이다.

## 1. 이미지 준비

Kubernetes 노드가 public registry에 접근할 수 있을 때 이미지를 미리 내려받는다.

```bash
sudo ctr --namespace k8s.io images pull docker.io/prom/prometheus:v3.13.1
sudo ctr --namespace k8s.io images pull docker.io/grafana/grafana:13.1.0
sudo ctr --namespace k8s.io images pull registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.19.1
sudo ctr --namespace k8s.io images list | grep -E 'prometheus|grafana|kube-state-metrics'
```

## 2. Local PV 디렉터리 준비

PV가 기존에 있어도 소유권이 정확히 교정되도록 `chown`과 `chmod`를 함께 실행한다.

```bash
sudo install -d -m 750 \
  /var/lib/lastdish/prometheus \
  /var/lib/lastdish/grafana

sudo chown 65534:65534 /var/lib/lastdish/prometheus
sudo chown 472:472 /var/lib/lastdish/grafana
sudo chmod 750 /var/lib/lastdish/prometheus /var/lib/lastdish/grafana
```

## 3. Grafana 관리자 Secret 생성

비밀번호는 저장소에 기록하지 않고 서버 터미널에서 직접 입력한다. 다음 명령은 bash와 zsh에서 모두 동작한다.

```bash
read_secret() {
  printf '%s' "$1" >&2
  stty -echo
  IFS= read -r REPLY
  stty echo
  printf '\n' >&2
}

trap 'stty echo 2>/dev/null' EXIT
trap 'exit 130' INT TERM HUP

while true; do
  read_secret 'Grafana admin password: '
  GRAFANA_ADMIN_PASSWORD=$REPLY
  read_secret 'Grafana admin password 확인: '
  GRAFANA_ADMIN_PASSWORD_CONFIRM=$REPLY

  if [[ -n "$GRAFANA_ADMIN_PASSWORD" && "$GRAFANA_ADMIN_PASSWORD" == "$GRAFANA_ADMIN_PASSWORD_CONFIRM" ]]; then
    break
  fi

  echo '비밀번호가 비어 있거나 서로 다릅니다. 다시 입력하세요.'
done

trap - EXIT INT TERM HUP
unset -f read_secret

kubectl create secret generic grafana-admin \
  --namespace=monitoring \
  --from-literal=admin-user=admin \
  --from-literal=admin-password="$GRAFANA_ADMIN_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

unset GRAFANA_ADMIN_PASSWORD GRAFANA_ADMIN_PASSWORD_CONFIRM
```

## 4. 적용

저장소 루트의 `infra/k8s`에서 순서대로 실행한다.

```bash
kubectl apply -f 00-cluster/namespaces.yaml
kubectl apply -f monitoring/storage.yaml
kubectl apply -f monitoring/prometheus-rbac.yaml
kubectl apply -f monitoring/kube-state-metrics.yaml
kubectl apply -f monitoring/prometheus-config.yaml
kubectl apply -f monitoring/prometheus.yaml
kubectl rollout status deployment/prometheus --namespace=monitoring --timeout=180s

kubectl apply -f monitoring/grafana-config.yaml
kubectl apply -f monitoring/grafana.yaml
kubectl rollout status deployment/grafana --namespace=monitoring --timeout=180s

kubectl apply -f networking/certificate.yaml
kubectl apply -f networking/ingress.yaml

kubectl get pod,service,pvc --namespace=monitoring
kubectl get certificate,ingress --namespace=monitoring
```

Prometheus는 다음 세 계층을 수집한다.

- Data EC2 exporter: PostgreSQL 2개와 Elasticsearch
- Kubernetes: kubelet cAdvisor의 Pod·컨테이너 CPU/메모리/네트워크와 kube-state-metrics의 객체 상태
- Spring: `prometheus.io/scrape=true` annotation이 있는 Gateway·Member·Core·Config Server의 `/actuator/prometheus`

Spring 메트릭은 각 서비스 이미지에 `micrometer-registry-prometheus`가 포함되고 Config 저장소가 `prometheus` Actuator endpoint를 노출한 뒤 정상 수집된다. 새 이미지 배포 전 annotation만 적용하면 해당 Spring target은 `DOWN`으로 표시될 수 있다.

## 5. 검증

Prometheus target 상태를 로컬 PC에서 확인한다.

```bash
ssh -L 9090:127.0.0.1:9090 lastdish \
  'kubectl port-forward --namespace=monitoring service/prometheus 9090:9090'
```

브라우저에서 `http://localhost:9090/targets`를 열고 네 job이 모두 `UP`인지 확인한다.

DNS와 인증서를 포함한 외부 접근은 `https://grafana.lastdish.kr`을 사용한다. 인증서가 아직 발급 중이면 별도 터미널에서 port-forward로 먼저 검증할 수 있다.

```bash
ssh -L 3000:127.0.0.1:3000 lastdish \
  'kubectl port-forward --namespace=monitoring service/grafana 3000:3000'
```

브라우저에서 `http://localhost:3000`을 열고 사용자명 `admin`과 Secret 생성 시 입력한 비밀번호로 로그인한다. Connections → Data sources에서 Prometheus가 기본 데이터 소스로 등록되고 연결 테스트가 성공하는지 확인한다.

## 설정 변경

Prometheus target이나 수집 주기를 변경한 뒤 ConfigMap과 Deployment를 다시 적용한다. ConfigMap만 변경하면 실행 중인 프로세스가 자동으로 다시 읽지 않으므로 rollout restart를 수행한다.

```bash
kubectl apply -f monitoring/prometheus-config.yaml
kubectl rollout restart deployment/prometheus --namespace=monitoring
kubectl rollout status deployment/prometheus --namespace=monitoring --timeout=180s
```
