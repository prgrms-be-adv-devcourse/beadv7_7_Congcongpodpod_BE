import 'package:shared_preferences/shared_preferences.dart';

/// "충전 끝나면 장바구니로 돌아가야 한다"는 의도를 브라우저에 저장해두는 플래그.
///
/// Toss 결제창은 성공/실패 모두 브라우저 페이지를 통째로 새로 로드하는 방식으로
/// 리다이렉트한다(deposit_charge_view_model.dart 참고) — 그 왕복 동안 앱의 인메모리
/// 상태(네비게이션 스택, Riverpod provider)가 전부 사라진다. 그래서 "어디서 왔는지"는
/// URL이나 메모리가 아니라 SharedPreferences처럼 리로드에도 살아남는 곳에 남겨야 한다
/// (토큰 저장과 같은 이유 — shared_prefs_token_storage.dart 참고).
const _returnToCartKey = 'deposit_charge_return_to_cart';

Future<void> markReturnToCartAfterCharge(SharedPreferences prefs) =>
    prefs.setBool(_returnToCartKey, true);

/// 플래그를 읽고 즉시 지운다("한 번만 쓰고 버리는" 의도라 읽자마자 지워야, 이후
/// 충전은 다시 기본 동작(예치금 화면으로)으로 돌아간다).
Future<bool> consumeReturnToCartAfterCharge(SharedPreferences prefs) async {
  final value = prefs.getBool(_returnToCartKey) ?? false;
  await prefs.remove(_returnToCartKey);
  return value;
}
