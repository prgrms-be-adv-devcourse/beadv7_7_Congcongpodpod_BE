# 로컬 빌드 및 실행 도구

`dev.sh`는 `dev/compose.yaml`을 사용해 서비스를 빌드·실행·중지하고 로컬 데이터를 초기화합니다. 어느 디렉터리에서 실행해도 `dev/.env`를 사용합니다.

로컬 Config Server 설정, JWT 키 생성기와 Kibana 도구는 [`dev/local/README.md`](local/README.md)에 정리되어 있습니다.

## 1. 환경변수 준비

macOS·Linux:

```bash
cp dev/.env.example dev/.env
```

실제 Toss 결제 테스트가 필요하면 `dev/.env`의 `TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY`를 테스트 키로 교체합니다. `dev/.env`는 Git에서 제외됩니다.

`dev.sh`는 Docker를 실행하기 전에 `.env.example` 기준 키 누락·중복과 필수값 공백을 검사합니다. `S3_ENABLED=true`이면 `S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`도 필수입니다. 검증 실패 시 오류를 한 번에 표시하고 빌드하지 않습니다.

`dev/.env.example`의 `SPRING_FLYWAY_LOCATIONS`는 개발·시연 seed를 활성화합니다. 전체 초기화 후 Member 1,000명(모두 SELLER 역할), 실제 서울 매장·상품 300개, 주문 300,000건, 정산 30,000건이 동일하게 생성됩니다. Member는 1~1000번 전부 생성되지만 매장·상품은 1~300번 회원에만 있습니다(회원=매장=상품 번호 동일). seed 없이 빈 스키마만 사용하려면 다음처럼 바꿉니다.

시연 서버에서도 Member·Core·Payment Deployment에 같은 `SPRING_FLYWAY_LOCATIONS` 값을 설정한 뒤 DB를 초기화하면 동일한 데이터가 생성됩니다. 운영 환경에는 seed location을 설정하지 않습니다.

```dotenv
SPRING_FLYWAY_LOCATIONS=classpath:db/migration
```

시연 계정은 `seller0001@seed.lastdish.kr`부터 `seller1000@seed.lastdish.kr`까지(4자리)이며 공통 비밀번호는 `LastDish!2026`입니다. 매장·상품이 있는 건 `seller0001`~`seller0300`뿐이고, `seller0301`~`seller1000`은 회원만 존재합니다. 모든 사용자의 최종 예치금은 1조 원이고 주문 사용·환불 원장과 일치합니다. 공개 매장명·주소·좌표는 [OpenStreetMap contributors](https://www.openstreetmap.org/copyright)의 2026-08-20 스냅샷이며, 실제 개인정보 대신 가상 소유자·전화·계좌 정보를 사용합니다.

## 2. 실행

| 작업 | 명령 |
| --- | --- |
| 전체 빌드·실행 | `./dev/dev.sh` |
| 특정 서비스 빌드·실행 | `./dev/dev.sh member-service` |
| 특정 서비스 중지 | `./dev/dev.sh stop member-service` |
| 특정 서비스 컨테이너 제거 | `./dev/dev.sh down member-service` |
| 전체 컨테이너 로그 | `./dev/dev.sh logs` |
| 특정 서비스 로그 | `./dev/dev.sh logs core-service` |
| 여러 서비스 로그 | `./dev/dev.sh logs core-service payment-service` |
| 최근 30줄부터 로그 | `./dev/dev.sh logs -line 30 core-service` |
| 전체 중지 | `./dev/dev.sh stop` |
| 전체 컨테이너·네트워크 제거 | `./dev/dev.sh down` |
| 도움말 | `./dev/dev.sh help` |

특정 서비스 실행 시 Compose의 `depends_on`에 선언된 DB, Config Server, Redis, Kafka 등 필요한 컨테이너도 함께 시작됩니다. 빌드에 성공하면 교체된 이전 서비스 이미지만 정리하며, 다른 컨테이너가 사용하는 이미지는 삭제하지 않습니다.
`down`에 서비스를 지정하면 해당 컨테이너만 중지·제거하고 볼륨·이미지·공용 네트워크는 유지합니다. 서비스를 생략하면 전체 컨테이너와 공용 네트워크를 제거합니다.
스크립트는 성공한 Docker 명령의 상세 출력을 숨기고 진행 단계만 표시합니다. 실패하면 해당 단계의 전체 출력을 표시합니다.

`logs`는 DB·Redis·Kafka·Elasticsearch를 포함한 전체 컨테이너의 최근 200줄부터 실시간 출력하며 `Ctrl+C`로 종료합니다. Compose 서비스명을 여러 개 지정하면 선택한 서비스 로그를 함께 볼 수 있습니다. `./dev/dev.sh logs -line 30 core-service`처럼 최근 출력 줄 수를 변경할 수 있으며 옵션은 서비스명 앞뒤 어디에 두어도 됩니다.

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

초기화는 데이터를 복구할 수 없게 삭제하므로 화면에 표시되는 `RESET <대상>` 확인 문구를 정확히 입력해야 실행됩니다.
개별 DB 초기화 시 해당 서비스 이미지를 다시 빌드하고 컨테이너를 교체하여 최신 Flyway migration과 seed를 빈 DB에 적용합니다.

## 4. 직접 Docker Compose 사용

스크립트 없이 실행할 때도 환경변수 파일을 명시합니다.

```bash
docker compose --env-file dev/.env --file dev/compose.yaml up -d --build
docker compose --env-file dev/.env --file dev/compose.yaml ps
docker compose --env-file dev/.env --file dev/compose.yaml down
```
