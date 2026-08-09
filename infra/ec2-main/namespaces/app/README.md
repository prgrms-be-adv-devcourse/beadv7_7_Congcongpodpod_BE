# App 서비스 배포 설정

ConfigMap과 Deployment/Service는 저장소의 YAML로 관리합니다. 실제 계정과 비밀번호는 YAML에 기록하지 않고 `app` 네임스페이스 Secret으로 생성합니다.

## Payment·AI Secret 생성

아래 명령은 Bash에서 실행합니다. 입력값은 셸 기록과 화면에 노출되지 않습니다.

```bash
read -r -p "Payment DB username: " PAYMENT_DB_USERNAME
read -r -s -p "Payment DB password: " PAYMENT_DB_PASSWORD
printf '\n'
read -r -p "AI DB username: " AI_DB_USERNAME
read -r -s -p "AI DB password: " AI_DB_PASSWORD
printf '\n'
read -r -p "Toss client key: " TOSS_CLIENT_KEY
read -r -s -p "Toss secret key: " TOSS_SECRET_KEY
printf '\n'
read -r -s -p "Elasticsearch password: " ELASTICSEARCH_PASSWORD
printf '\n'

kubectl create secret generic payment-db-credentials \
  --namespace=app \
  --from-literal=username="$PAYMENT_DB_USERNAME" \
  --from-literal=password="$PAYMENT_DB_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic ai-db-credentials \
  --namespace=app \
  --from-literal=username="$AI_DB_USERNAME" \
  --from-literal=password="$AI_DB_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic payment-external-credentials \
  --namespace=app \
  --from-literal=toss-client-key="$TOSS_CLIENT_KEY" \
  --from-literal=toss-secret-key="$TOSS_SECRET_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic elasticsearch-credentials \
  --namespace=app \
  --from-literal=password="$ELASTICSEARCH_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -

unset PAYMENT_DB_USERNAME PAYMENT_DB_PASSWORD \
  AI_DB_USERNAME AI_DB_PASSWORD \
  TOSS_CLIENT_KEY TOSS_SECRET_KEY ELASTICSEARCH_PASSWORD
```

`redis-auth`, `ghcr-pull-secret`은 기존 app 서비스와 동일한 Secret을 공유합니다.

## 최초 적용 확인

```bash
kubectl apply -f infra/ec2-main/namespaces/app/configmaps/
kubectl apply -f infra/ec2-main/namespaces/app/workloads/payment-service.yaml
kubectl apply -f infra/ec2-main/namespaces/app/workloads/ai-service.yaml

kubectl rollout status deployment/payment-service --namespace=app --timeout=180s
kubectl rollout status deployment/ai-service --namespace=app --timeout=180s

kubectl get pods,services --namespace=app
```

Gateway의 `/api/v1/payments/**`는 Payment 코드 이전이 끝날 때까지 Core로 라우팅합니다. AI API 경로도 Controller가 추가된 뒤 Gateway route를 등록합니다.
