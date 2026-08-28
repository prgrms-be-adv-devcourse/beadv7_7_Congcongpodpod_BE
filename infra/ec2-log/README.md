# LastDish EC2 Log (Home Server)

`ec2-log`는 Home Mini PC의 배포 이름이다. Loki, Prometheus, Grafana를 Docker Compose로 실행한다. Prometheus는 Main EC2를 경유하는 제한된 SSH 터널로 Main/Data 메트릭을 직접 수집한다.

Caddy는 관측 도메인과 함께 `lastdish.kr`, `www.lastdish.kr`의 정적 웹도 제공한다. Expo Web 빌드 결과는 Home 서버의 `web/dist`에 배포하며 Caddy 컨테이너에 읽기 전용으로 마운트한다.

## 네트워크 계약

- `ec2-log → ec2-main`: SSH TCP `22`; 전용 키는 `6443`, `9100`과 Data EC2 exporter 포트로의 전달만 허용한다.
- `ec2-main → log.lastdish.kr`: HTTPS `443` (Main 로그와 Data relay 로그 push)
- `ec2-data → ec2-main`: VPC 내부 TCP `30100` (Data Alloy → Main Alloy relay)
- 인터넷 → `grafana.lastdish.kr`: HTTPS `443`
- 인터넷 → `lastdish.kr`, `www.lastdish.kr`: HTTPS `443`
- `3000`, `3100`, `9090`은 인터넷에 직접 공개하지 않는다.

## Home 서버 준비

```bash
cp infra/ec2-log/.env.example infra/ec2-log/.env
chmod 600 infra/ec2-log/.env
```

`.env`의 관리자 비밀번호를 반드시 교체한다.

## Kubernetes 읽기 자격증명

Main EC2에서 읽기 전용 계정과 1년 토큰을 생성한다. 토큰과 CA는 Git에 커밋하지 않고 Home 서버의 `infra/ec2-log/secrets/kubernetes/`에만 둔다.

```bash
kubectl apply -f infra/ec2-main/namespaces/monitoring/rbac/prometheus-reader.yaml
mkdir -p infra/ec2-log/secrets/kubernetes
kubectl create token prometheus-remote --namespace=monitoring --duration=8760h \
  > infra/ec2-log/secrets/kubernetes/token
kubectl config view --raw --minify --flatten \
  -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' \
  | base64 --decode > infra/ec2-log/secrets/kubernetes/ca.crt
chmod 600 infra/ec2-log/secrets/kubernetes/token infra/ec2-log/secrets/kubernetes/ca.crt
```

Kubernetes API 인증서가 `kubernetes` 이름을 포함하지 않으면 `prometheus/prometheus.yml`의 `server_name`을 인증서 SAN에 맞춘다. 인증서 검증을 끄지 않는다.

## 데이터 이전과 절체

처음부터 시작해도 된다면 데이터 복사는 생략한다. 기존 이력을 보존할 때는 먼저 수집기를 중지해 파일이 바뀌지 않게 한 뒤 `rsync`한다.

1. Home Compose를 `docker compose --env-file .env config`로 검증한다.
2. Home 직접 수집 대상이 모두 `UP`인지 확인한다.
3. 기존 데이터를 보존한다면 Compose named volume에 복원한다.
4. Home Compose를 시작한다.
5. Main Alloy 목적지를 `https://log.lastdish.kr/loki/api/v1/push`, Data Alloy 목적지를 Main relay `http://10.30.1.212:30100/loki/api/v1/push`로 적용한다.
6. Prometheus targets, Loki 로그, Grafana 대시보드를 확인한다.
7. 검증 후 기존 Kubernetes 객체와 Data Loki 컨테이너를 제거한다.

Grafana SQLite DB까지 복사하면 기존 사용자 설정을 유지한다. 복사하지 않아도 저장소의 대시보드와 데이터 소스는 자동 provisioning된다.

## Grafana 대시보드 동기화 원칙

`infra/ec2-log/grafana/dashboards/*.json`을 대시보드의 기준 원본으로 관리한다.

- 대시보드는 가능하면 로컬 JSON을 먼저 수정한 뒤 Home 서버에 배포한다.
- Home Grafana UI 또는 서버에서 대시보드를 직접 수정했다면 같은 작업 안에서 JSON을 export하여 로컬의 대응 파일도 반드시 갱신한다.
- 서버·Grafana SQLite DB에만 존재하는 변경을 완료 상태로 두지 않는다. provisioning 또는 재배포 시 사라질 수 있다.
- 로컬 JSON은 `jq empty <dashboard-file>.json`으로 검증한 후 서버의 `/home/lastdish-log/ec2-log/grafana/dashboards/`에 동기화한다.
- 서버 배포 후 Grafana가 대시보드를 다시 읽었는지와 변경 패널의 쿼리 결과를 확인한다.

```bash
jq empty infra/ec2-log/grafana/dashboards/<dashboard-file>.json
scp infra/ec2-log/grafana/dashboards/<dashboard-file>.json \
  lastdish-log:/home/lastdish-log/ec2-log/grafana/dashboards/
```

## 실행 및 검증

웹은 저장소 루트에서 빌드한 뒤 Home 서버의 `ec2-log/web/dist`에 동기화한다. `EXPO_PUBLIC_*` 환경변수는 빌드 시 번들에 포함되므로 빌드 전에 운영 값을 설정한다.

```bash
cd frontend/react-native
npm ci
npm run web:build
```

```bash
cd infra/ec2-log
docker compose --env-file .env config --quiet
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose --env-file .env ps

curl --fail http://127.0.0.1:9090/-/ready
curl --fail http://127.0.0.1:3100/ready
curl --fail http://127.0.0.1:3000/api/health
```

Prometheus의 `/targets`에서 모든 job이 `UP`인지 확인하고 `https://grafana.lastdish.kr`도 확인한다.
