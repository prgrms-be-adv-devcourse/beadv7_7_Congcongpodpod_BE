# LastDish 운영 CLI와 Metrics Server

## 저장소 백업

서버에서 사용하는 `ldm` 전체 설정은 다음 파일에 백업한다.

```text
infra/k8s/tools/lastdish.zshrc
```

이 파일에는 LastDish 메뉴, Kubernetes 조회·관리 명령, DB 접속 별칭이 포함된다. 실제 Secret 값, PAT, PEM, DB 비밀번호는 저장하지 않는다.

서버에 복원한다. 기존 설정은 먼저 백업한다.

```bash
cp ~/.zshrc ~/.zshrc.backup
install -m 600 infra/k8s/tools/lastdish.zshrc ~/.zshrc
source ~/.zshrc
```

`ldm`은 SSH 대화형 로그인에서 자동으로 한 번 실행된다. 수동 실행도 가능하다.

```bash
ldm
```

필요 도구:

- Zsh와 Oh My Zsh
- `kubectl`
- `jq`
- `openssl`
- GNU `numfmt`
- `free`

## Metrics Server 설치

`ldm → 클러스터 상태 → 리소스 사용량`에서 노드와 Pod의 CPU·메모리를 표시하려면 Metrics API가 필요하다. 현재 검증 버전은 Metrics Server `v0.8.1`이다.

공식 manifest를 적용한다.

```bash
kubectl apply -f \
  https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.8.1/components.yaml
```

Deployment 준비 상태를 확인한다.

```bash
kubectl rollout status deployment/metrics-server \
  --namespace=kube-system \
  --timeout=120s
```

### kubelet 인증서에 IP SAN이 없는 클러스터

다음 오류가 Metrics Server 로그에 있을 때만 TLS 검증 예외를 적용한다.

```text
x509: cannot validate certificate for <node-ip> because it doesn't contain any IP SANs
```

```bash
kubectl patch deployment metrics-server \
  --namespace=kube-system \
  --type=json \
  --patch='[
    {
      "op": "add",
      "path": "/spec/template/spec/containers/0/args/-",
      "value": "--kubelet-insecure-tls"
    }
  ]'

kubectl rollout status deployment/metrics-server \
  --namespace=kube-system \
  --timeout=120s
```

`--kubelet-insecure-tls`는 Metrics Server가 kubelet 인증서를 검증하지 않게 한다. 인증서에 올바른 IP SAN을 넣는 것이 장기적으로 더 안전하다. 이 옵션은 kubelet IP SAN 오류가 확인된 내부 클러스터에서만 사용한다.

## 검증

APIService가 사용 가능한지 확인한다.

```bash
kubectl get apiservice v1beta1.metrics.k8s.io -o json |
  jq -r '.status.conditions[] |
    select(.type == "Available") |
    "Available=\(.status) reason=\(.reason)"'
```

기대 결과:

```text
Available=True reason=Passed
```

노드와 Pod 지표를 확인한다.

```bash
kubectl top nodes
kubectl top pods --all-namespaces
```

CLI 화면을 확인한다.

```bash
source ~/.zshrc
ldm
```

`ldm` 전체 메뉴는 Rust TUI(`~/.local/bin/ldm`)로 실행한다. 다음 작업 영역을 Rust에서 제공한다.

메뉴와 데이터 화면은 Codex 앱 스타일의 공통 셸과 LastDish 주황색 포인트를 사용한다. 88열 이상에서는 왼쪽 작업 영역 사이드바와 오른쪽 콘텐츠 패널을 함께 표시하고, 그보다 좁으면 콘텐츠 단일 패널로 자동 전환한다. 터미널 높이에 따라 메뉴 표시 개수와 데이터 행 수도 다시 계산한다.

SSH PTY가 실제 창보다 작은 폭을 전달할 수 있어 터미널 에뮬레이터의 실제 행·열도 조회한다. 지원되는 터미널에서는 입력이 없는 시점에 750ms 간격으로 크기를 다시 확인해 창 변경을 자동 반영한다. 반영이 늦으면 `W`로 즉시 다시 감지한다. 화면은 실제 터미널 폭 전체를 사용하며 물리적인 창보다 더 넓게 만드는 수동 폭 보정은 제공하지 않는다. 화면별 키 도움말은 잘리지 않도록 상단에 고정한다.

- 클러스터 상태와 실시간 리소스. 애플리케이션 Pod에는 `app` Namespace 서비스와 `platform/config-server`를 함께 표시한다.
- ConfigMap/Secret 조회·생성·삭제와 TLS 인증서 정보
- 애플리케이션: Gateway/Member/Core/Config Server 로그, Spring 재시작, 배포 상태와 이미지 재배포
- 데이터베이스: Member/Core DB 로그, DB 초기화, 내장 SQL 콘솔 접속
- Rollout 상태와 이미지 재배포

`애플리케이션 → 재배포 → 이미지 재배포 → App 전체`는 `Config Server → Gateway → Member → Core` 순서로 실행한다. 각 Deployment에 `rollout restart`를 요청한 뒤 최대 300초 동안 Ready를 확인하고, 성공한 경우에만 다음 서비스로 진행한다. 실패하면 이후 서비스는 건드리지 않고 중단한다.

`애플리케이션 → 재시작`은 이미지와 Pod를 교체하지 않고 컨테이너의 Java PID 1에 `SIGTERM`을 보내 Kubernetes 컨테이너 재시작을 유도한다. restart count 증가와 Ready 복귀를 최대 300초 확인한다. `App 전체`는 이미지 재배포와 같은 순서로 하나씩 처리한다.

`애플리케이션 → 재배포 → 배포 상태 확인`은 Kubernetes 영문 원문 대신 다음 한글 판정과 복제본 수치를 표시한다.

- `정상`: 목표 수만큼 새 버전·준비 완료·서비스 가능 복제본이 확보됨
- `진행 중`: 새 Pod 생성 또는 기존 Pod 교체 중
- `대기`: 새 버전 생성 후 시작 검사나 준비 검사 통과를 기다리는 중
- `오류`: 복제본 생성 실패 또는 배포 제한 시간 초과

화면에서는 `설정 반영 → 새 버전 → 준비 완료 → 서비스 가능 → 사용 불가` 순서로 확인한다. 상태 조건과 Kubernetes 사유도 한글 설명으로 변환한다.

실시간 리소스는 alternate screen에서 변경된 행만 갱신하므로 화면이 스크롤되거나 전체가 깜빡이지 않는다. 2초마다 Metrics API를 다시 조회하며 `Q` 또는 `Esc`로 이전 메뉴로 돌아간다.

최근 로그는 대상 선택 후 `1~5000` 사이 줄 수를 입력한다. 빈 값은 `200`줄이다. 긴 로그와 명령 결과는 원문을 보존하며 방향키로 가로·세로 스크롤한다. 출력 상단의 주황색 화살표, 현재 열·행 범위와 스크롤바로 남은 내용을 확인할 수 있다. 터미널 크기가 바뀌면 표시 범위와 스크롤 한계를 즉시 다시 계산한다. 로그 화면은 기본적으로 사이드바를 접어 전체 폭을 사용하며 `Tab`으로 다시 표시한다. 실시간 로그도 동일한 TUI 내부에서 갱신하며 `F`로 최신 행 추적, `Q`, `Esc`, `Ctrl+C`로 추적만 종료한다. `WARN`과 `ERROR` 행은 색상으로 구분한다.

데이터베이스의 `Member DB 접속`, `Core DB 접속`은 `ldm` 내부의 지속형 `psql` 콘솔로 열린다. SQL 입력 후 `Enter`로 실행하고 `↑↓`로 결과를 세로 스크롤하며 `←→`로 긴 결과를 가로 스크롤한다. `Esc` 또는 `\q`로 데이터베이스 콘솔만 닫고 `ldm`으로 돌아간다.

상단 도움말의 `포커스:` 문구와 색으로 현재 키 입력 대상을 구분한다. 왼쪽 탐색에 포커스가 있으면 사이드바를 주황색으로 강조한다. 오른쪽 작업 메뉴·입력창·결과 화면을 조작할 때는 사이드바를 회색 문맥 표시로 바꾼다. 터미널 크기는 자동 감지하며 `Ctrl+R`로 즉시 다시 감지할 수 있다.

- 노드 총 메모리, 사용량, 가용량, 사용률과 메모리 게이지
- 운영 Pod(`app`, `data`, `platform`)의 CPU와 메모리
- Kubernetes 시스템 Pod의 CPU와 메모리
- KST 기준 조회 시각

키보드:

- `1`: 운영 Pod
- `2`: 시스템 Pod
- `3`: 운영·시스템 전체
- `R`: 즉시 갱신
- `Q`, `Esc`: 종료

Rust 바이너리는 저장소에서 제외된 `infra/k8s/tools/ldm-metrics`에 로컬 백업한다. 서버용 Linux x86_64 바이너리는 다음처럼 빌드하고 배포한다.

```bash
docker run --platform linux/amd64 --rm \
  -v "$PWD/infra/k8s/tools/ldm-metrics:/src" \
  -w /src \
  rust:1.96-bookworm \
  cargo build --release

ssh lastdish 'mkdir -p ~/.local/bin'
scp infra/k8s/tools/ldm-metrics/target/release/ldm \
  lastdish:~/.local/bin/ldm
ssh lastdish 'chmod 755 ~/.local/bin/ldm'
```

Rust 바이너리가 없으면 `ldm`은 기존 Zsh 메뉴로 자동 fallback한다. `kmetrics` 별칭은 Rust 바이너리의 `--metrics` 모드로 실시간 리소스 화면을 바로 연다.

Metrics API를 사용할 수 없으면 서버 전체 메모리만 표시하고 Pod 지표가 대기 중임을 안내한다.

## 설정 리소스 분류

`ldm → 설정`은 리소스를 실행 시점에 Kubernetes API에서 다시 조회한다.

- 운영 설정: `app`, `data`, `platform`
- 시스템 설정: 그 외 Namespace와 모든 `kube-root-ca.crt`
- TLS 인증서: 타입이 `kubernetes.io/tls`인 Secret
- 전체 목록: 분류 없이 모든 Namespace 조회

TLS 인증서 메뉴는 인증서 Subject, Issuer, Serial, 유효기간만 표시한다. `tls.key` 개인키는 출력하지 않는다.

## 제거

Metrics Server가 더 이상 필요하지 않으면 같은 버전의 manifest로 제거한다.

```bash
kubectl delete -f \
  https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.8.1/components.yaml
```
