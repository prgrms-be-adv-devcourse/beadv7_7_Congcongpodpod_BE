# 로컬 통합 환경

Config Server, Gateway, Member, Core를 Docker Compose로 함께 실행한다. 운영 Config 저장소나 PAT를 사용하지 않는다.

## 실행

저장소 루트에서:

```bash
docker compose up --build -d
docker compose ps
```

## 확인

```bash
curl -fsS http://localhost:8888/member-service/default
curl -fsS http://localhost:8080/api/members/hello
curl -fsS http://localhost:8080/api/core/hello
```

직접 호출:

```bash
curl -fsS http://localhost:8081/api/members/hello
curl -fsS http://localhost:8082/api/core/hello
```

## 로그와 종료

```bash
docker compose logs -f gateway-service member-service core-service
docker compose down
```

이미지까지 삭제하려면:

```bash
docker compose down --rmi local
```

`infra/local/config`에는 로컬 개발용 공개 설정만 저장한다. 비밀번호, 토큰, Private key는 추가하지 않는다.
