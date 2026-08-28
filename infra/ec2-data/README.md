# LastDish Data Server

데이터 EC2 `10.30.2.93`에서 PostgreSQL과 단일 노드 Elasticsearch를 Docker Compose로 실행한다.

| 컨테이너 | 포트 | 메모리 상한 | JVM heap |
| --- | ---: | ---: | ---: |
| `member-postgresql` | `5432` | `512MiB` | - |
| `core-postgresql` | `5433` | `640MiB` | - |
| `elasticsearch` | `9200` | `1,152MiB` | `512MiB` |
| `member-postgresql-exporter` | `9187` | `32MiB` | - |
| `core-postgresql-exporter` | `9188` | `32MiB` | - |
| `payment-postgresql-exporter` | `9189` | `32MiB` | - |
| `ai-postgresql-exporter` | `9190` | `32MiB` | - |
| `elasticsearch-exporter` | `9114` | `32MiB` | - |
| `node-exporter` | `9100` | `64MiB` | - |
| `cadvisor` | `8080` | `256MiB` | - |
| `alloy` | 외부 노출 없음 | `128MiB` | - |

Compose의 `mem_limit`은 컨테이너가 사용할 수 있는 메모리의 하드 상한이다. 상한에 도달하면 프로세스가 OOM 종료될 수 있으므로 운영 중 사용량과 재시작 횟수를 함께 관찰한다.

PostgreSQL Exporter는 서비스별 DB 계정으로 접속한다. 해당 계정에 서버 전체 WAL 파일 목록을 읽는 권한을 부여하지 않기 위해 `wal` collector를 비활성화한다. 현재 Grafana 대시보드에서 WAL 파일 크기·세그먼트 지표는 사용하지 않으며, DB 연결·트랜잭션·잠금·캐시·I/O 지표 수집에는 영향이 없다.

PostgreSQL의 `log_line_prefix`에는 `db=<database> user=<user>`를 포함한다. Data EC2 Alloy는 이 값을 파싱해 `application` 라벨을 `member_db`, `core_db`, `payment_db`, `ai_db`로 설정하고 VPC 내부의 Main Alloy relay(`10.30.1.212:30100`)로 전송한다. Main Alloy가 Home Loki로 중계하므로 Data EC2에 인터넷 egress는 필요 없다. 물리적으로 `core-postgresql` 하나에 저장된 세 논리 DB도 Grafana 서비스 로그에서 분리해 조회할 수 있다. Redis와 Kafka는 Kubernetes `data` namespace의 Pod 라벨을 사용한다.

Elasticsearch는 기본 `9.5.0-amd64` 이미지에 같은 버전의 `analysis-nori`를 설치한 `lastdish-elasticsearch:9.5.0-nori` 이미지를 사용한다. Private EC2는 외부 레지스트리에 연결하지 않는다. `elasticsearch/Dockerfile`과 `analysis-nori-9.5.0.zip`을 함께 보관해야 오프라인 재빌드할 수 있다. Elasticsearch 버전을 올릴 때는 기본 이미지, 플러그인 ZIP, Dockerfile, Compose 태그를 모두 같은 버전으로 변경한다.

## 준비

EC2 보안 그룹에서 `5432`, `5433`, `9200`은 Main EC2만 허용한다. `8080`, `9100`, `9114`, `9187`, `9188`, `9189`, `9190`은 VPN의 `ec2-log`만 추가 허용한다. 데이터·Exporter 포트를 `0.0.0.0/0`에 공개하지 않는다.

```bash
sudo install -d -o 999 -g 999 -m 700 \
  /var/lib/lastdish/postgresql/member \
  /var/lib/lastdish/postgresql/core

# Elasticsearch 컨테이너 사용자(UID 1000)가 데이터를 기록할 디렉터리를 만든다.
sudo install -d -o 1000 -g 0 -m 750 /var/lib/lastdish/elasticsearch

# 디렉터리가 이미 존재하면 install -d가 기존 소유권을 유지할 수 있으므로 명시적으로 교정한다.
sudo chown 1000:0 /var/lib/lastdish/elasticsearch
sudo chmod 750 /var/lib/lastdish/elasticsearch

# Alloy가 컨테이너별 로그 읽기 위치를 재시작 후에도 이어서 사용하도록 저장합니다.
sudo install -d -o root -g root -m 750 /var/lib/lastdish/alloy

# Elasticsearch가 사용하는 가상 메모리 맵 한도를 현재 실행 중인 커널에 적용한다.
sudo sysctl -w vm.max_map_count=1048576

# EC2 재부팅 후에도 같은 값이 적용되도록 영구 설정한다.
echo 'vm.max_map_count=1048576' | sudo tee /etc/sysctl.d/99-elasticsearch.conf
sudo sysctl --system

cp .env.example .env
chmod 600 .env
```

`install -d`는 PostgreSQL 데이터를 영구 저장할 호스트 디렉터리를 생성한다.

- `-o 999 -g 999`: `postgres:17` 컨테이너의 PostgreSQL 사용자 UID/GID에 소유권을 부여한다.
- `-m 700`: 디렉터리 소유자만 읽기, 쓰기, 접근할 수 있게 제한한다.
- Member와 Core 데이터를 서로 다른 디렉터리에 보관한다.
- 컨테이너를 삭제하거나 재생성해도 데이터는 EC2의 `/var/lib/lastdish/postgresql` 아래에 유지된다.
- 기존 디렉터리가 있어도 데이터를 삭제하지 않는다.

`.env`에 Member/Core/Payment/AI PostgreSQL 계정과 비밀번호, `ELASTIC_PASSWORD`를 설정한다. Compose의 `database-initializer`가 Core PostgreSQL 기동 후 `payment_db`, `ai_db`와 전용 계정을 자동 생성한다. 이미 존재하면 건너뛰므로 반복 실행해도 안전하다. Elasticsearch 기본 관리자 계정명은 `elastic`이다. `ELASTICSEARCH_BIND_ADDRESS`와 `EXPORTER_BIND_ADDRESS`는 데이터 EC2의 사설 IP로 유지한다. `.env`는 Git에서 제외된다.

## Elasticsearch Nori 이미지

인터넷에 연결된 로컬 PC에서 Elasticsearch와 정확히 같은 버전의 Nori 플러그인을 받는다.

```bash
curl --fail --location \
  --output infra/ec2-data/elasticsearch/analysis-nori-9.5.0.zip \
  https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-nori/analysis-nori-9.5.0.zip
```

`infra/ec2-data` 전체를 Data EC2의 `/home/ec2-user/database`에 동기화한 뒤 오프라인 이미지를 빌드한다. 서버에 기본 `docker.elastic.co/elasticsearch/elasticsearch:9.5.0-amd64` 이미지가 먼저 존재해야 한다.

```bash
cd /home/ec2-user/database
docker image inspect docker.elastic.co/elasticsearch/elasticsearch:9.5.0-amd64 >/dev/null
docker build --pull=false \
  --tag lastdish-elasticsearch:9.5.0-nori \
  ./elasticsearch
```

현재 Data EC2의 Docker Buildx는 Compose가 요구하는 버전보다 낮으므로 `docker compose build` 대신 위 `docker build` 명령을 사용한다.

Nori 이미지는 외부에서 pull할 수 없는 로컬 이미지이므로 삭제하면 위 명령으로 다시 빌드한다. 플러그인 ZIP은 재빌드를 위해 삭제하지 않는다.

인터넷에 연결된 환경에서 나머지 이미지를 미리 내려받으려면 다음 명령을 실행한다. `elasticsearch`는 `pull_policy: never`이므로 대상에서 제외된다.

```bash
docker compose --env-file .env pull --ignore-buildable
```

## 실행 및 확인

```bash
docker compose --env-file .env up -d
docker compose --env-file .env ps

docker compose --env-file .env exec member-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'

docker compose --env-file .env exec core-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'

# 같은 PostgreSQL 인스턴스 안의 독립 논리 DB를 확인한다.
# core-postgresql 컨테이너에는 Payment·AI 비밀번호 환경변수를 주입하지 않으므로
# 초기 PostgreSQL 관리자 계정으로 DB 소유자와 접속 가능 여부를 검사한다.
docker compose --env-file .env exec core-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d payment_db -c "select current_database(), pg_get_userbyid(datdba) as owner from pg_database where datname = current_database();"'

docker compose --env-file .env exec core-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d ai_db -c "select current_database(), pg_get_userbyid(datdba) as owner from pg_database where datname = current_database();"'

# 컨테이너에 주입된 비밀번호로 Elasticsearch 인증과 클러스터 상태를 확인한다.
docker compose --env-file .env exec elasticsearch \
  sh -ec 'curl --fail --user "elastic:$ELASTIC_PASSWORD" \
    "http://localhost:9200/_cluster/health?pretty"'

# Nori 플러그인 설치 여부를 확인한다.
docker compose --env-file .env exec elasticsearch \
  bin/elasticsearch-plugin list | grep -x analysis-nori

# 한국어 문장이 형태소 토큰으로 분석되는지 확인한다.
docker compose --env-file .env exec elasticsearch \
  sh -ec 'curl --fail --user "elastic:$ELASTIC_PASSWORD" \
    -H "Content-Type: application/json" \
    -X POST "http://localhost:9200/_analyze?pretty" \
    -d '\''{"analyzer":"nori","text":"맛있는 한식당을 추천해 주세요"}'\'''

# 각 exporter가 Prometheus 형식의 메트릭을 반환하는지 확인한다.
curl --fail --silent http://10.30.2.93:9187/metrics | grep '^pg_up '
curl --fail --silent http://10.30.2.93:9188/metrics | grep '^pg_up '
curl --fail --silent http://10.30.2.93:9189/metrics | grep '^pg_up '
curl --fail --silent http://10.30.2.93:9190/metrics | grep '^pg_up '
curl --fail --silent http://10.30.2.93:9114/metrics | grep '^elasticsearch_clusterinfo_up'
curl --fail --silent http://10.30.2.93:9100/metrics | grep '^node_uname_info'

```

`docker compose ps`에서 Elasticsearch가 `healthy`가 되면 정상이다. 단일 노드에서는 복제본을 배치할 다른 노드가 없어 상태가 `yellow`일 수 있으며, 서비스 장애를 뜻하지 않는다.

종료한다.

```bash
docker compose --env-file .env down
```

Kubernetes `app` 네임스페이스의 `member-db-credentials`, `core-db-credentials`, `payment-db-credentials`, `ai-db-credentials` Secret에는 Compose `.env`와 동일한 사용자명과 비밀번호를 설정한다. Elasticsearch는 URL `http://10.30.2.93:9200`, 사용자명 `elastic`, `.env`의 `ELASTIC_PASSWORD`와 동일한 비밀번호를 `elasticsearch-credentials` Secret에 등록한다.

## Exporter

Exporter는 저장소가 아니라 PostgreSQL과 Elasticsearch의 상태를 읽어 Prometheus 형식으로 제공하는 경량 프로세스다. Prometheus를 설치한 뒤 다음 target을 등록한다.

| Prometheus job | Target |
| --- | --- |
| Member PostgreSQL | `10.30.2.93:9187` |
| Core PostgreSQL | `10.30.2.93:9188` |
| Payment PostgreSQL | `10.30.2.93:9189` |
| AI PostgreSQL | `10.30.2.93:9190` |
| Elasticsearch | `10.30.2.93:9114` |
| Data EC2 Node Exporter | `10.30.2.93:9100` |

초기 구성은 기존 PostgreSQL 계정과 Elasticsearch `elastic` 계정을 재사용한다. 운영 권한을 최소화하려면 PostgreSQL에는 `pg_monitor` 역할만 가진 exporter 전용 계정, Elasticsearch에는 `remote_monitoring_collector` 역할만 가진 전용 계정을 생성한 뒤 Compose 인증값을 교체한다.

Node Exporter는 별도 계정이나 Secret 없이 Data EC2 호스트의 `/`를 읽기 전용으로 마운트해 CPU·RAM·디스크·I/O 지표를 제공합니다. `node-exporter` 컨테이너만 갱신하려면 `docker compose --env-file .env up -d node-exporter`를 실행합니다.
