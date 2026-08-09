# 로컬 빌드 및 실행 도구

`dev.sh`와 `dev.ps1`은 저장소 루트의 `compose.yaml`을 사용해 서비스를 빌드·실행·중지하고 로컬 데이터를 초기화합니다. 어느 디렉터리에서 실행해도 `dev/.env`를 사용합니다.

## 1. 환경변수 준비

macOS·Linux:

```bash
cp dev/.env.example dev/.env
```

Windows PowerShell:

```powershell
Copy-Item dev/.env.example dev/.env
```

실제 Toss 결제 테스트가 필요하면 `dev/.env`의 `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`를 테스트 키로 교체합니다. `dev/.env`는 Git에서 제외됩니다.

## 2. 실행

| 작업 | macOS·Linux | Windows PowerShell |
| --- | --- | --- |
| 전체 빌드·실행 | `./dev/dev.sh` | `.\dev\dev.ps1` |
| 특정 서비스 빌드·실행 | `./dev/dev.sh member-service` | `.\dev\dev.ps1 member-service` |
| 특정 서비스 중지 | `./dev/dev.sh stop member-service` | `.\dev\dev.ps1 stop member-service` |
| 전체 중지 | `./dev/dev.sh stop` | `.\dev\dev.ps1 stop` |
| 컨테이너·네트워크 제거 | `./dev/dev.sh down` | `.\dev\dev.ps1 down` |
| 도움말 | `./dev/dev.sh help` | `.\dev\dev.ps1 help` |

특정 서비스 실행 시 Compose의 `depends_on`에 선언된 DB, Config Server, Redis, Kafka 등 필요한 컨테이너도 함께 시작됩니다. 빌드에 성공하면 교체된 이전 서비스 이미지만 정리하며, 다른 컨테이너가 사용하는 이미지는 삭제하지 않습니다.

## 3. 데이터 초기화

```bash
./dev/dev.sh reset member-db
./dev/dev.sh reset core-db
./dev/dev.sh reset payment-db
./dev/dev.sh reset ai-db
./dev/dev.sh reset kafka
./dev/dev.sh reset redis
./dev/dev.sh reset elasticsearch
./dev/dev.sh reset all
```

Windows에서는 `./dev/dev.sh`를 `.\dev\dev.ps1`로 바꿉니다. 초기화는 데이터를 복구할 수 없게 삭제하므로 화면에 표시되는 `RESET <대상>` 확인 문구를 정확히 입력해야 실행됩니다.

## 4. 직접 Docker Compose 사용

스크립트 없이 실행할 때도 환경변수 파일을 명시합니다.

```bash
docker compose --env-file dev/.env up -d --build
docker compose --env-file dev/.env ps
docker compose --env-file dev/.env down
```
