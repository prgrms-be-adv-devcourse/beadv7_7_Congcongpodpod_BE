import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/payment.dart';
import '../deposit_providers.dart';
import 'payment_providers.dart';

part 'deposit_charge_success_view_model.g.dart';

/// 충전 성공 리다이렉트 화면(B14 연장)의 상태. Toss가 넘겨준 `paymentKey`/`orderId`/
/// `amount`로 승인(`POST /payments/approve`)을 호출해 예치금 충전을 확정한다.
@riverpod
class DepositChargeSuccessViewModel extends _$DepositChargeSuccessViewModel {
  @override
  FutureOr<PaymentApprove?> build() => null;

  Future<void> approve({
    required String paymentKey,
    required String orderId,
    required num amount,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final result = await ref
          .read(paymentRepositoryProvider)
          .approve(paymentKey: paymentKey, orderId: orderId, amount: amount);
      // 잔액이 바뀌었으니 예치금 화면(B13)이 다음에 열릴 때 최신값을 다시 받아오게 한다.
      ref.invalidate(depositBalanceProvider);
      ref.invalidate(depositHistoryProvider);
      return result;
    });
  }
}
