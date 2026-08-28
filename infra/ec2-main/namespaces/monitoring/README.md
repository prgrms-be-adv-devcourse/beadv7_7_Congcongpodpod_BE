# Main EC2 관측 수집 계층

Main EC2에는 저장·조회 서버를 두지 않고 Alloy, kube-state-metrics, Node Exporter만 유지한다. Prometheus와 Grafana는 `infra/ec2-log`의 Home 서버에서 실행한다.

```bash
kubectl apply -f infra/ec2-main/bootstrap/namespaces.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/kube-state-metrics.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/node-exporter.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/alloy.yaml
kubectl apply -f infra/ec2-main/namespaces/monitoring/prometheus-reader.yaml
kubectl apply -f infra/ec2-main/networking/certificate.yaml
kubectl apply -f infra/ec2-main/networking/ingress.yaml
```

Home Prometheus는 제한된 SSH 터널로 Main Kubernetes API·Node Exporter와 Data EC2 exporter를 직접 수집한다. Main Alloy는 HTTPS로 `log.lastdish.kr`에 로그를 전송한다. 인터넷 egress가 없는 Data EC2의 Alloy는 VPC 내부 `10.30.1.212:30100`으로 push하고 Main Alloy가 Home Loki로 중계한다.

이전과 검증을 마친 뒤 기존 저장 컴포넌트를 제거한다. `Retain` PV의 실제 디렉터리는 백업 검증 전 삭제하지 않는다.

```bash
kubectl delete deployment,service prometheus --namespace=monitoring --ignore-not-found
kubectl delete pvc prometheus-data --namespace=monitoring --ignore-not-found
```
