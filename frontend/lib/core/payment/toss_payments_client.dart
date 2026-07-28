// `TossPaymentsWebClient`의 플랫폼별 진입점. 실제 구현은 웹에서만 의미가 있다
// (`dart:js_interop` 기반, toss_payments_web_client.dart) — 그 외 플랫폼은
// 호출 시 예외를 던지는 stub(toss_payments_stub_client.dart)으로 빠진다.
export 'toss_payments_stub_client.dart'
    if (dart.library.js_interop) 'toss_payments_web_client.dart';
