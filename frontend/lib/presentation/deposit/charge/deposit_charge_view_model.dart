import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/payment/toss_payments_client.dart';
import '../../../core/routing/route_paths.dart';
import '../../member/member_repository_provider.dart';
import 'payment_providers.dart';

part 'deposit_charge_view_model.g.dart';

/// 예치금 충전 화면(B14)의 상태. 결제 준비(`ready`) 성공 후 Toss 결제창을 띄우는
/// 순간 브라우저가 페이지를 통째로 떠나기 때문에(성공 시 successUrl로 리다이렉트),
/// 이 Notifier가 "성공"으로 끝나는 경우는 사실상 없다 — 성공 처리는
/// deposit_charge_success_view_model.dart(승인 API 호출)가 별도 화면에서 이어받는다.
/// 여기서 잡는 에러는 결제창을 띄우기 전 단계(준비 API 실패, 결제창 자체를 못 연 경우)뿐이다.
@riverpod
class DepositChargeViewModel extends _$DepositChargeViewModel {
  @override
  FutureOr<void> build() {}

  Future<void> charge(num amount) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final ready = await ref
          .read(paymentRepositoryProvider)
          .ready(amount: amount);
      final member = await ref.read(myInfoProvider.future);

      // Toss가 successUrl에 `?paymentKey=...&orderId=...&amount=...`를 붙여 리다이렉트한다.
      // 이 앱은 hash 기반 라우팅(usePathUrlStrategy 안 씀)이라, 그 쿼리는 `#` 앞
      // origin 경로에 붙는다 — 성공 화면(deposit_charge_success_screen.dart)에서
      // go_router가 아니라 `Uri.base.queryParameters`로 직접 읽는 이유가 이것이다.
      final origin = Uri.base.origin;
      await TossPaymentsWebClient.requestCardPayment(
        tossClientKey: ready.tossClientKey,
        merchantOrderId: ready.merchantOrderId,
        amount: ready.amount,
        orderName: '예치금 충전',
        successUrl: '$origin/#${RoutePaths.depositChargeSuccess}',
        failUrl: '$origin/#${RoutePaths.depositChargeFail}',
        customerEmail: member.email,
        customerName: member.name,
      );
    });
  }
}
