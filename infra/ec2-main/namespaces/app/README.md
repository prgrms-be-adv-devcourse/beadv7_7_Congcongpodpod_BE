# App 서비스 배포 설정

ConfigMap과 Deployment/Service는 저장소의 YAML로 관리합니다. 실제 계정과 비밀번호는 YAML에 기록하지 않고 `app` 네임스페이스 Secret으로 생성합니다.

비밀이 아닌 DB·Redis·Kafka·Elasticsearch·S3 접속값은
`../runtime/config/app-runtime-config.yaml` 하나에서 관리합니다. 변경 후 app Deployment를
롤아웃해야 환경변수가 다시 주입됩니다.

## 서비스 Secret 적용

`infra/ec2-main/namespaces/runtime/examples/app.env.example`을 `env/app.env`로 복사해 모든 값을 채우고 JWT PEM
두 개도 `env` 디렉터리에 둡니다. 값 파일과 PEM은 Git에서 제외합니다.

```bash
cp infra/ec2-main/namespaces/runtime/examples/app.env.example infra/ec2-main/namespaces/runtime/env/app.env
cp infra/ec2-main/namespaces/runtime/examples/registry.env.example infra/ec2-main/namespaces/runtime/env/registry.env
chmod 600 infra/ec2-main/namespaces/runtime/env/*.env infra/ec2-main/namespaces/runtime/env/*.pem
./infra/ec2-main/apply-secrets.sh app
```

이 명령은 `app-runtime-secrets`에 애플리케이션 환경변수와 JWT 키를 반영하고,
`registry.env`로 `ghcr-pull-secret`도 함께 갱신합니다. 각 Deployment는 필요한 키만
선택해 사용합니다. cert-manager가 관리하는 `api-lastdish-kr-tls`는 별도로 유지합니다.

## 최초 적용 확인

```bash
kubectl apply -f infra/ec2-main/namespaces/runtime/config/app-runtime-config.yaml
kubectl apply -f infra/ec2-main/namespaces/app/payment-service.yaml
kubectl apply -f infra/ec2-main/namespaces/app/ai-service.yaml

kubectl rollout status deployment/payment-service --namespace=app --timeout=180s
kubectl rollout status deployment/ai-service --namespace=app --timeout=180s

kubectl get pods,services --namespace=app
```

Gateway의 `/api/v1/payments/**`는 Payment 코드 이전이 끝날 때까지 Core로 라우팅합니다. AI API 경로도 Controller가 추가된 뒤 Gateway route를 등록합니다.
