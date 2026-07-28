import 'dart:js_interop';

/// Toss Payments 일반결제 SDK v2(`https://js.tosspayments.com/v2/standard`,
/// web/index.html에 스크립트 태그로 로드됨)를 `dart:js_interop`으로 감싼 것.
///
/// `tosspayments_widget_sdk_flutter` 패키지가 있지만 Android/iOS만 지원하고
/// Flutter Web은 안 된다 — 그래서 위젯 임베드 대신 Toss가 호스팅하는 결제창으로
/// 리다이렉트되는 "일반결제" 방식을 JS SDK 직접 호출로 붙인다. 결제 성공/실패 시
/// `successUrl`/`failUrl`로 전체 페이지 리다이렉트가 일어나므로, 이후 처리는
/// 그 경로에 대응하는 화면(deposit_charge_success_screen.dart)이 쿼리파라미터를
/// 읽어서 이어간다 — 이 파일은 "결제창 띄우기"까지만 담당한다.
///
/// 카드 결제(`CARD`)만 지원한다 — 계좌이체/가상계좌 등 나머지 결제수단은
/// 지금 범위 밖(시연 목적상 카드 하나면 충분).

@JS('TossPayments')
external JSTossPaymentsInstance _tossPayments(JSString clientKey);

extension type JSTossPaymentsInstance._(JSObject _) implements JSObject {
  external JSPaymentInstance payment(JSPaymentInitOptions options);
}

extension type JSPaymentInitOptions._(JSObject _) implements JSObject {
  external factory JSPaymentInitOptions({required JSString customerKey});
}

extension type JSPaymentInstance._(JSObject _) implements JSObject {
  external JSPromise<JSAny?> requestPayment(
    JSRequestCardPaymentOptions options,
  );
}

extension type JSRequestCardPaymentOptions._(JSObject _) implements JSObject {
  external factory JSRequestCardPaymentOptions({
    required JSString method,
    required JSAmount amount,
    required JSString orderId,
    required JSString orderName,
    required JSString successUrl,
    required JSString failUrl,
    required JSString customerEmail,
    required JSString customerName,
    required JSCardOptions card,
  });
}

extension type JSAmount._(JSObject _) implements JSObject {
  external factory JSAmount({
    required JSString currency,
    required JSNumber value,
  });
}

extension type JSCardOptions._(JSObject _) implements JSObject {
  external factory JSCardOptions({
    required JSBoolean useEscrow,
    required JSString flowMode,
    required JSBoolean useCardPoint,
    required JSBoolean useAppCardOnly,
  });
}

/// Dart 쪽에서 쓰는 얇은 클라이언트. `dart:js_interop` 타입을 이 파일 밖으로
/// 새어나가지 않게 막는 역할도 겸한다 — 호출하는 쪽(view model)은 평범한 Dart
/// 타입(String/num)만 넘기면 된다.
class TossPaymentsWebClient {
  /// [customerKey]는 결제창 세션을 구분하는 키다. 회원별 고유값이면 되고,
  /// 이메일·전화번호처럼 유추 가능한 값은 쓰지 말라고 Toss가 권장한다
  /// (샘플 코드 주석) — 그래서 memberId를 그대로 안 쓰고 매 충전 시도마다
  /// 새 랜덤 문자열을 만들어 쓴다.
  ///
  /// 성공하면 브라우저가 [successUrl]로, 실패하면 [failUrl]로 전체 페이지
  /// 리다이렉트된다 — 이 Future는 리다이렉트가 일어나기 전에 발생하는 에러
  /// (예: 사용자가 결제창을 직접 닫음)만 잡아낸다.
  static Future<void> requestCardPayment({
    required String tossClientKey,
    required String merchantOrderId,
    required num amount,
    required String orderName,
    required String successUrl,
    required String failUrl,
    required String customerEmail,
    required String customerName,
  }) async {
    final tossPayments = _tossPayments(tossClientKey.toJS);
    final payment = tossPayments.payment(
      JSPaymentInitOptions(customerKey: _randomCustomerKey().toJS),
    );

    await payment
        .requestPayment(
          JSRequestCardPaymentOptions(
            method: 'CARD'.toJS,
            amount: JSAmount(
              currency: 'KRW'.toJS,
              value: amount.toDouble().toJS,
            ),
            orderId: merchantOrderId.toJS,
            orderName: orderName.toJS,
            successUrl: successUrl.toJS,
            failUrl: failUrl.toJS,
            customerEmail: customerEmail.toJS,
            customerName: customerName.toJS,
            card: JSCardOptions(
              useEscrow: false.toJS,
              flowMode: 'DEFAULT'.toJS,
              useCardPoint: false.toJS,
              useAppCardOnly: false.toJS,
            ),
          ),
        )
        .toDart;
  }

  static String _randomCustomerKey() {
    final millis = DateTime.now().millisecondsSinceEpoch;
    final rand = (millis * 2654435761) % 0xFFFFFFFF;
    return 'customer-$millis-${rand.toRadixString(16)}';
  }
}
