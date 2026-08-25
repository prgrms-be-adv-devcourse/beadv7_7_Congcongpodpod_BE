# s3-storage

이미지 업로드·다운로드를 위한 S3 객체 저장소와 Presigned URL 계약을 제공하는 Spring Boot 자동 구성 모듈입니다.

## 제공 기능

- 업로드·다운로드 Presigned URL 생성
- 이미지 MIME type과 최대 크기 검증
- 업로드 resource type과 상태 관리
- 객체 메타데이터 저장
- AWS S3 또는 호환 endpoint 연결

## 사용

```gradle
implementation project(':modules:s3-storage')
```

핵심 설정:

| 환경변수 | 설명 |
| --- | --- |
| `S3_ENABLED` | S3 기능 활성화 |
| `S3_BUCKET` | bucket 이름 |
| `AWS_REGION` | AWS region |
| `AWS_ACCESS_KEY_ID` | 접근 키 |
| `AWS_SECRET_ACCESS_KEY` | 비밀 키 |
| `S3_ENDPOINT` | 선택적 S3 호환 endpoint |
| `S3_PRESIGNED_URL_EXPIRATION` | URL 유효 시간 |
| `S3_MAX_UPLOAD_SIZE` | 최대 업로드 크기 |

자격 증명은 저장소에 커밋하지 않습니다. `S3_ENABLED=false`인 로컬 실행과 실제 S3 연동 실행의 필수 환경변수 계약은 `dev/.env.example`과 `dev/README.md`를 기준으로 확인하세요.
