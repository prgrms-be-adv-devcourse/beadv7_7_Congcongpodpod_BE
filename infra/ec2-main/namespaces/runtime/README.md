# 네임스페이스 Secret 관리

Secret 값은 네임스페이스별 env 파일 하나로 관리합니다. 예제 파일을 복사해 값을 채우고
권한을 `600`으로 제한합니다. `app`에는 JWT PEM 두 개도 필요합니다.
env 파일은 첫 번째 `=` 뒤의 내용을 그대로 저장하므로 값을 따옴표로 감싸지 않습니다.
값이 `=+`로 시작하면 `AWS_SECRET_ACCESS_KEY==+...`처럼 입력합니다.
OpenAI를 사용하는 경우 `app.env`의 `OPENAI_API_KEY`에 실제 키를 입력합니다. 모델명은
`OPENAI_EMBEDDING_MODEL`, `OPENAI_CHAT_MODEL`로 관리하며 기본값은 예제 파일에 있습니다.

```bash
cd infra/ec2-main/namespaces/runtime
cp examples/app.env.example env/app.env
cp examples/app-openai.env.example env/app-openai.env
cp examples/registry.env.example env/registry.env
cp examples/data.env.example env/data.env
cp examples/platform.env.example env/platform.env
cp examples/monitoring.env.example env/monitoring.env
chmod 600 env/*.env env/*.pem
```

모든 namespace의 비밀값과 런타임 ConfigMap을 이 디렉터리 한 곳에서 관리합니다.

```text
namespaces/runtime/
├── config/
│   └── app-runtime-config.yaml
├── env/
│   ├── app.env
│   ├── app-openai.env
│   ├── registry.env
│   ├── access-private-key.pem
│   └── access-public-key.pem
└── examples/
    ├── app.env.example
    ├── app-openai.env.example
    └── registry.env.example
```

각 파일을 수정한 뒤 저장소 루트에서 해당 네임스페이스만 반영합니다.

```bash
./infra/ec2-main/apply-secrets.sh app
./infra/ec2-main/apply-secrets.sh data
./infra/ec2-main/apply-secrets.sh platform
./infra/ec2-main/apply-secrets.sh monitoring
```

생성되는 Opaque Secret은 각각 `app-runtime-secrets`, `data-runtime-secrets`,
`platform-runtime-secrets`, `monitoring-runtime-secrets`입니다. 적용 명령은 Secret만
갱신하며 Pod를 자동 재시작하지 않습니다. 변경 후 사용하는 Deployment를 롤아웃합니다.

`registry.env`는 `app`과 `platform`이 공유합니다. `app` 또는 `platform`을 적용하면 해당
네임스페이스의 `ghcr-pull-secret`도 같은 명령에서 함께 갱신됩니다.

```bash
kubectl rollout restart deployment --namespace=app
kubectl rollout restart deployment/redis --namespace=data
kubectl rollout restart deployment/config-server --namespace=platform
kubectl rollout restart deployment/alloy --namespace=monitoring
```

`ghcr-pull-secret`은 Docker Registry 타입이라 Opaque Secret과 객체는 분리하지만 적용
명령은 통합합니다. `api-lastdish-kr-tls`는 cert-manager가 관리합니다. 새 Secret으로
전환한 Deployment가 정상화되기 전에는 기존 Secret을 삭제하지 않습니다.
