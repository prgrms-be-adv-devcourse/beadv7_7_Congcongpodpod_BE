# LastDish PostgreSQL

데이터베이스 EC2 `10.30.2.93`에서 Member PostgreSQL은 `5432`, Core PostgreSQL은 `5433` 포트로 실행한다.

| 컨테이너 | 포트 | 메모리 상한 |
| --- | ---: | ---: |
| `member-postgresql` | `5432` | `512MiB` |
| `core-postgresql` | `5433` | `640MiB` |

Compose의 `mem_limit`은 컨테이너가 사용할 수 있는 메모리의 하드 상한이다. 상한에 도달하면 프로세스가 OOM 종료될 수 있으므로 운영 중 사용량과 재시작 횟수를 함께 관찰한다.

## 준비

EC2 보안 그룹에서 `5432`, `5433` 인바운드는 Kubernetes 서버의 사설 IP 또는 보안 그룹에만 허용한다.

```bash
sudo install -d -o 999 -g 999 -m 700 \
  /var/lib/lastdish/postgresql/member \
  /var/lib/lastdish/postgresql/core

cp .env.example .env
chmod 600 .env
```

`install -d`는 PostgreSQL 데이터를 영구 저장할 호스트 디렉터리를 생성한다.

- `-o 999 -g 999`: `postgres:17` 컨테이너의 PostgreSQL 사용자 UID/GID에 소유권을 부여한다.
- `-m 700`: 디렉터리 소유자만 읽기, 쓰기, 접근할 수 있게 제한한다.
- Member와 Core 데이터를 서로 다른 디렉터리에 보관한다.
- 컨테이너를 삭제하거나 재생성해도 데이터는 EC2의 `/var/lib/lastdish/postgresql` 아래에 유지된다.
- 기존 디렉터리가 있어도 데이터를 삭제하지 않는다.

`.env`의 두 비밀번호를 설정한다. 이 파일은 Git에서 제외된다.

## 실행 및 확인

```bash
docker compose --env-file .env up -d
docker compose --env-file .env ps

docker compose --env-file .env exec member-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'

docker compose --env-file .env exec core-postgresql \
  sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select current_database(), current_user;"'
```

종료한다.

```bash
docker compose --env-file .env down
```

Kubernetes `app` 네임스페이스의 `member-db-credentials`, `core-db-credentials` Secret에는 각각 Compose `.env`와 동일한 사용자명과 비밀번호를 설정한다.
