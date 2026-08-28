# Data 네임스페이스

Redis와 Kafka처럼 애플리케이션이 공통으로 사용하는 데이터 인프라를 배치한다. PostgreSQL은 이 네임스페이스가 아니라 Private Data EC2에서 실행한다.

## Redis

Redis는 원본 저장소가 아닌 캐시로 사용한다. RDB/AOF 영속화를 비활성화했으므로 Pod가 재생성되면 캐시 데이터는 사라진다.

### 1. 인증 Secret 생성

`infra/ec2-main/namespaces/runtime/examples/data.env.example`을 `env/data.env`로 복사하고 app의
`REDIS_PASSWORD`와 같은 값을 입력한 뒤 적용한다. 스크립트가 Redis ACL 파일을 임시로
생성해 `data-runtime-secrets`에 함께 저장한다.

```bash
cp infra/ec2-main/namespaces/runtime/examples/data.env.example infra/ec2-main/namespaces/runtime/env/data.env
chmod 600 infra/ec2-main/namespaces/runtime/env/data.env
./infra/ec2-main/apply-secrets.sh data
```

### 2. Redis 적용

```bash
kubectl apply -f infra/ec2-main/namespaces/data/redis.yaml
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
| 비밀번호 | `data-runtime-secrets.REDIS_PASSWORD` Secret |
| 메모리 상한 | `384Mi` |
| Redis 데이터 상한 | `256mb` |
| 제거 정책 | `allkeys-lru` |

App Deployment는 `app-runtime-secrets.REDIS_PASSWORD`를 참조한다. Redis 주소·포트·사용자명은 `app/app-runtime-config` ConfigMap에서 가져온다.

## Kafka

Kafka는 단일 KRaft broker/controller로 실행한다. 클러스터 내부 통신만 허용하며 현재 단계에서는 SASL과 TLS를 적용하지 않는다.

### 1. 데이터 디렉터리 준비

`kafka.yaml`의 Local PersistentVolume이 사용하는 디렉터리를 Kubernetes 노드에 만든다. 공식 Apache Kafka 이미지는 UID/GID `1000`으로 실행된다.

```bash
sudo install -d -o 1000 -g 1000 -m 750 /var/lib/lastdish/kafka
```

### 2. Kafka 적용

```bash
kubectl apply -f infra/ec2-main/namespaces/data/kafka.yaml
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
| SSH 터널 bootstrap server | Kafka Service ClusterIP의 `9094` 포트 |
| 로컬 Docker Compose | `localhost:9092` |
| 모드 | 단일 KRaft broker/controller |
| 보안 프로토콜 | 클러스터 내부 `PLAINTEXT` |
| JVM heap | `512Mi` |
| Pod 메모리 상한 | `1280Mi` |
| PV | `10Gi`, `Retain` |
| 로그 보존 | 각 partition당 최대 72시간 또는 5GiB |
| 자동 topic 생성 | 비활성화 |

App Deployment는 `app/app-runtime-config.KAFKA_BOOTSTRAP_SERVERS`를 참조하며 Kafka를 기본 이벤트 발행 방식으로 사용한다.

JetBrains Kafka 도구에서는 bootstrap server를 Kafka Service ClusterIP의 `9094` 포트로,
SSH 구성을 Main EC2로, 로컬 포트를 `9094`로 설정한다. SSH 전용 listener가 반환하는
`127.0.0.1:9094`가 IDE의 로컬 터널로 연결되므로 별도 DNS 설정은 필요 없다.
