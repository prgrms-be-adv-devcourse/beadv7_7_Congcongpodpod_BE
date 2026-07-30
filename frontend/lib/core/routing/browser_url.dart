import 'dart:js_interop';

/// hash 라우팅 + Toss 리다이렉트 조합 때문에 생기는 문제 하나를 처리한다:
/// Toss가 successUrl/failUrl에 붙이는 `paymentKey`/`orderId`/`amount` 쿼리는
/// `#` 앞(진짜 브라우저 query)에 실리는데, go_router 네비게이션은 `#` 뒤(fragment)만
/// 바꿔서 이 쿼리가 주소창에 계속 남는다. 값을 다 읽은 직후 이걸로 지운다 —
/// 브라우저 히스토리/화면공유/제3자 리소스 Referer로 결제 식별자가 새어나가는
/// 시간을 최소화하기 위함(치명적 위험은 아니지만 저비용으로 막을 수 있어서 처리).
@JS('history.replaceState')
external void _replaceState(JSAny? data, String title, String url);

/// 현재 주소창에서 `#` 앞 query만 제거하고 fragment(go_router 경로)는 그대로 둔다.
void stripLeakedQueryFromUrl() {
  final fragment = Uri.base.fragment;
  final cleanUrl = '${Uri.base.origin}/${fragment.isEmpty ? '' : '#$fragment'}';
  _replaceState(null, '', cleanUrl);
}
