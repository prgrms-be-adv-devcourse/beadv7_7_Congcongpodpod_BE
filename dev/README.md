# 로컬 빌드 및 실행 도구

`dev.sh`와 `dev.ps1`은 `dev/compose.yaml`을 사용해 서비스를 빌드·실행·중지하고 로컬 데이터를 초기화합니다. 어느 디렉터리에서 실행해도 `dev/.env`를 사용합니다.

로컬 Config Server 설정, JWT 키 생성기와 Kibana 도구는 [`dev/local/README.md`](local/README.md)에 정리되어 있습니다.

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

`dev/.env.example`의 `SPRING_FLYWAY_LOCATIONS`는 개발·시연 seed를 활성화합니다. 전체 초기화 후 Member 300명, 실제 서울 매장·상품 300개, 주문 300,000건, 정산 30,000건이 동일하게 생성됩니다. seed 없이 빈 스키마만 사용하려면 다음처럼 바꿉니다.

시연 서버에서도 Member·Core·Payment Deployment에 같은 `SPRING_FLYWAY_LOCATIONS` 값을 설정한 뒤 DB를 초기화하면 동일한 데이터가 생성됩니다. 운영 환경에는 seed location을 설정하지 않습니다.

```dotenv
SPRING_FLYWAY_LOCATIONS=classpath:db/migration
```

시연 계정은 `seller001@seed.lastdish.kr`부터 `seller300@seed.lastdish.kr`까지이며 공통 비밀번호는 `LastDish!2026`입니다. 사용자 1~150의 최종 예치금은 10,000,000원, 사용자 151~300은 0원이고 주문 사용·환불 원장과 일치합니다. 공개 매장명·주소·좌표는 [OpenStreetMap contributors](https://www.openstreetmap.org/copyright)의 2026-08-20 스냅샷이며, 실제 개인정보 대신 가상 소유자·전화·계좌 정보를 사용합니다.

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
docker compose --env-file dev/.env --file dev/compose.yaml up -d --build
docker compose --env-file dev/.env --file dev/compose.yaml ps
docker compose --env-file dev/.env --file dev/compose.yaml down
```
