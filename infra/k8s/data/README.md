# Data 네임스페이스

Redis와 Kafka처럼 애플리케이션이 공통으로 사용하는 데이터 인프라를 배치한다. PostgreSQL은 이 네임스페이스가 아니라 Private Data EC2에서 실행한다.

## Redis

Redis는 원본 저장소가 아닌 캐시로 사용한다. RDB/AOF 영속화를 비활성화했으므로 Pod가 재생성되면 캐시 데이터는 사라진다.

### 1. 인증 Secret 생성

비밀번호와 ACL 파일은 저장소에 커밋하지 않는다. 운영 서버에서 관리자가 비밀번호를 직접 입력하고 Secret으로 저장한다.

아래 명령은 `lastdish` 서버의 bash와 zsh에서 모두 실행할 수 있다. 비밀번호는 입력 화면에 표시되지 않는다. 공백 없는 32자 이상의 값을 권장한다.

```bash
read_secret() {
  printf '%s' "$1" >&2
  stty -echo
  IFS= read -r REPLY
  stty echo
  printf '\n' >&2
}

# 입력 중 중단되어도 터미널 입력 표시를 원상 복구합니다.
trap 'stty echo 2>/dev/null' EXIT
trap 'exit 130' INT TERM HUP

while true; do
  read_secret 'Redis password: '
  REDIS_PASSWORD=$REPLY
  read_secret 'Redis password 확인: '
  REDIS_PASSWORD_CONFIRM=$REPLY

  if [[ -n "$REDIS_PASSWORD" && "$REDIS_PASSWORD" == "$REDIS_PASSWORD_CONFIRM" ]]; then
    break
  fi

  echo "비밀번호가 비어 있거나 서로 다릅니다. 다시 입력하세요."
done

trap - EXIT INT TERM HUP
unset -f read_secret

REDIS_PASSWORD_FILE=$(mktemp)
REDIS_ACL_FILE=$(mktemp)

chmod 600 "$REDIS_PASSWORD_FILE" "$REDIS_ACL_FILE"
printf '%s' "$REDIS_PASSWORD" > "$REDIS_PASSWORD_FILE"
printf 'user default on >%s ~* &* +@all\n' "$REDIS_PASSWORD" > "$REDIS_ACL_FILE"

for namespace in data app; do
  kubectl create secret generic redis-auth \
    --namespace="$namespace" \
    --from-file=password="$REDIS_PASSWORD_FILE" \
    --from-file=users.acl="$REDIS_ACL_FILE" \
    --dry-run=client -o yaml | kubectl apply -f -
done

rm -f "$REDIS_PASSWORD_FILE" "$REDIS_ACL_FILE"
unset REDIS_PASSWORD REDIS_PASSWORD_CONFIRM REDIS_PASSWORD_FILE REDIS_ACL_FILE
```

Redis Pod는 `data/redis-auth`, Gateway·Member·Core는 `app/redis-auth`를 참조한다. Kubernetes Secret은 다른 네임스페이스에서 직접 참조할 수 없으므로 동일한 자격증명을 두 네임스페이스에 생성한다.

Secret 키만 확인하고 실제 값은 출력하지 않는다.

```bash
kubectl describe secret redis-auth --namespace=data
kubectl describe secret redis-auth --namespace=app
```

### 2. Redis 적용

```bash
kubectl apply -f data/redis.yaml
kubectl rollout status deployment/redis --namespace=data --timeout=180s
kubectl get pod,service --namespace=data
```

### 3. 연결 검증

Redis Pod 내부에서 Secret을 사용해 인증과 쓰기·읽기를 확인한다. 검증 키는 마지막에 삭제한다.

```bash
REDIS_POD=$(kubectl get pod \
  --namespace=data \
  --selector=app=redis \
  --output=jsonpath='{.items[0].metadata.name}')

kubectl exec --namespace=data "$REDIS_POD" -- sh -c \
  'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'

kubectl exec --namespace=data "$REDIS_POD" -- sh -c \
  'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli set lastdish:health ok EX 60'

kubectl exec --namespace=data "$REDIS_POD" -- sh -c \
  'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli get lastdish:health'

kubectl exec --namespace=data "$REDIS_POD" -- sh -c \
  'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli del lastdish:health'
```

### 연결 정보

| 항목 | 값 |
|---|---|
| Kubernetes DNS | `redis.data.svc.cluster.local` |
| 같은 `data` 네임스페이스에서 사용하는 이름 | `redis` |
| 포트 | `6379` |
| 사용자 | `default` |
| 비밀번호 | `redis-auth.password` Secret |
| 메모리 상한 | `384Mi` |
| Redis 데이터 상한 | `256mb` |
| 제거 정책 | `allkeys-lru` |

Gateway·Member·Core Deployment는 `app/redis-auth.password`를 `REDIS_PASSWORD` 환경변수로 참조한다. Redis 주소·포트·사용자명은 `app/redis-client-config` ConfigMap에서 가져온다.

## Kafka

Kafka는 단일 KRaft broker/controller로 실행한다. 클러스터 내부 통신만 허용하며 현재 단계에서는 SASL과 TLS를 적용하지 않는다.

### 1. 데이터 디렉터리 준비

`kafka.yaml`의 Local PersistentVolume이 사용하는 디렉터리를 Kubernetes 노드에 만든다. 공식 Apache Kafka 이미지는 UID/GID `1000`으로 실행된다.

```bash
sudo install -d -o 1000 -g 1000 -m 750 /var/lib/lastdish/kafka
```

### 2. Kafka 적용

```bash
kubectl apply -f data/kafka.yaml
kubectl rollout status statefulset/kafka --namespace=data --timeout=300s
kubectl get pod,service,pv,pvc --namespace=data
```

### 3. Broker 검증

검증 topic을 만들고 메시지 한 건을 발행·소비한 뒤 삭제한다.

```bash
kubectl exec --namespace=data kafka-0 -- \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic lastdish-health \
  --partitions 1 \
  --replication-factor 1

printf 'ok\n' | kubectl exec -i --namespace=data kafka-0 -- \
  /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic lastdish-health

kubectl exec --namespace=data kafka-0 -- \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic lastdish-health \
  --from-beginning \
  --max-messages 1

kubectl exec --namespace=data kafka-0 -- \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic lastdish-health
```

### 연결 및 운영 정보

| 항목 | 값 |
|---|---|
| Kubernetes bootstrap server | `kafka.data.svc.cluster.local:9092` |
| 로컬 Docker Compose | `localhost:9092` |
| 모드 | 단일 KRaft broker/controller |
| 보안 프로토콜 | 클러스터 내부 `PLAINTEXT` |
| JVM heap | `512Mi` |
| Pod 메모리 상한 | `1280Mi` |
| PV | `10Gi`, `Retain` |
| 로그 보존 | 각 partition당 최대 72시간 또는 5GiB |
| 자동 topic 생성 | 비활성화 |

Member·Core Deployment는 `app/kafka-client-config.bootstrap-servers`를 `KAFKA_BOOTSTRAP_SERVERS` 환경변수로 참조한다. 현재 `event.publisher=spring`이므로 Kafka 발행·소비 코드를 추가하기 전까지는 연결 정보만 준비된 상태다.
