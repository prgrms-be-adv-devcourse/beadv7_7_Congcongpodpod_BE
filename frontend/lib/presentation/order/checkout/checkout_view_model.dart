import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/order.dart';
import '../order_repository_provider.dart';

part 'checkout_view_model.g.dart';

/// 체크아웃 화면의 상태 — signup_view_model.dart와 비슷하지만, 성공했을 때 생성된
/// [Order]를 그대로 들고 있는다(확인 다이얼로그에 주문번호/픽업시간 등을 보여줘야 해서).
@riverpod
class CheckoutViewModel extends _$CheckoutViewModel {
  @override
  FutureOr<Order?> build() => null; // 아직 아무 시도도 안 함.

  Future<void> submit({
    required int storeId,
    required int dishId,
    required String phone,
    required String dishName,
    required int quantity,
    required num unitPrice,
    required String pickupStartAt,
    required String pickupEndAt,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(orderRepositoryProvider)
          .createOrder(
            storeId: storeId,
            dishId: dishId,
            phone: phone,
            dishName: dishName,
            quantity: quantity,
            unitPrice: unitPrice,
            pickupStartAt: pickupStartAt,
            pickupEndAt: pickupEndAt,
          ),
    );
  }
}
