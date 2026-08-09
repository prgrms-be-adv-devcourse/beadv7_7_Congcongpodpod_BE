# Monitoring 네임스페이스

Prometheus가 Data EC2의 PostgreSQL·Elasticsearch exporter를 수집하고 Grafana가 Prometheus를 기본 데이터 소스로 조회한다. 둘 다 Kubernetes 내부에서 실행하며 Grafana는 초기 검증 단계에서 외부에 공개하지 않는다.

## 구성

`alloy.yaml`은 Kubernetes API를 통해 `app`, `platform` namespace의 Pod 로그를 수집하고 Data EC2 Loki(`10.30.2.93:3100`)로 전달합니다. 노드 파일시스템을 마운트하지 않으므로 단일 Alloy Deployment로 운영합니다.

| 구성 요소 | 이미지 | 메모리 상한 | 저장소 |
| --- | --- | ---: | ---: |
| Prometheus | `prom/prometheus:v3.13.1` | `384Mi` | `10Gi`, 7일 또는 9GB |
| Grafana | `grafana/grafana:13.1.0` | `1Gi` | `1Gi` |
| kube-state-metrics | `registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.19.1` | `128Mi` | - |
| Node Exporter | `quay.io/prometheus/node-exporter:v1.12.1` | `64Mi` | - |

전체 Prometheus target의 수집·규칙 평가 주기는 `10s`입니다. 짧은 정산·배치 부하를 관찰할 수 있지만 30초 주기보다 저장량과 scrape 요청이 약 3배 증가하므로 Prometheus 메모리와 TSDB 사용량을 함께 확인합니다.

Prometheus target은 `10.30.2.93:9187`(Member PostgreSQL), `10.30.2.93:9188`(Core PostgreSQL), `10.30.2.93:9114`(Elasticsearch)이다.

## 1. 이미지 준비

Kubernetes 노드가 public registry에 접근할 수 있을 때 이미지를 미리 내려받는다.

```bash
sudo ctr --namespace k8s.io images pull docker.io/prom/prometheus:v3.13.1
sudo ctr --namespace k8s.io images pull docker.io/grafana/grafana:13.1.0
sudo ctr --namespace k8s.io images pull registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.19.1
sudo ctr --namespace k8s.io images pull quay.io/prometheus/node-exporter:v1.12.1
sudo ctr --namespace k8s.io images list | grep -E 'prometheus|grafana|kube-state-metrics|node-exporter'
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

저장소 루트에서 다음 순서로 실행한다. monitoring은 제품별 디렉터리로 나뉘며 `storage.yaml`만 Prometheus와 Grafana가 공유한다.

```bash
kubectl apply -f infra/ec2-main/bootstrap/namespaces.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/storage.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/prometheus/rbac.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/exporters/kube-state-metrics.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/exporters/node-exporter.yaml
kubectl rollout status daemonset/node-exporter --namespace=monitoring --timeout=180s
kubectl apply -f infra/ec2-main/namespaces/monitoring/prometheus/config.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/prometheus/workload.yaml
kubectl rollout status deployment/prometheus --namespace=monitoring --timeout=180s

kubectl apply -f infra/ec2-main/namespaces/monitoring/grafana/config.yaml
kubectl replace -f infra/ec2-main/namespaces/monitoring/grafana/dashboards.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/grafana/workload.yaml
kubectl rollout status deployment/grafana --namespace=monitoring --timeout=180s

kubectl apply -f infra/ec2-main/networking/certificate.yaml
kubectl apply -f infra/ec2-main/networking/ingress.yaml

kubectl get pod,service,pvc --namespace=monitoring
kubectl get certificate,ingress --namespace=monitoring
```

Prometheus는 다음 세 계층을 수집한다.

- Data EC2 exporter: PostgreSQL 2개와 Elasticsearch
- Kubernetes: kubelet cAdvisor의 Pod·컨테이너 자원, kube-state-metrics의 객체 상태, Node Exporter의 호스트 CPU·메모리·디스크·네트워크
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
kubectl apply -f infra/ec2-main/namespaces/monitoring/prometheus/config.yaml
kubectl rollout restart deployment/prometheus --namespace=monitoring
kubectl rollout status deployment/prometheus --namespace=monitoring --timeout=180s
```
