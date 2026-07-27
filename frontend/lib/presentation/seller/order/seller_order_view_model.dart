import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/order.dart';
import '../../order/order_repository_provider.dart';

part 'seller_order_view_model.g.dart';

/// 매장 주문 목록(S3). order_list_view_model.dart(구매자용)과 같은 구조 —
/// 상태 필터는 별도 Provider로 관리하고, 이 Provider가 그 값을 구독해서
/// 필터가 바뀌면 자동으로 다시 조회한다.
@riverpod
Future<List<Order>> sellerOrderList(Ref ref, int storeId) {
  final status = ref.watch(selectedSellerOrderStatusProvider);
  return ref.watch(orderRepositoryProvider).getStoreOrders(storeId: storeId, status: status);
}

/// S3 상태 필터 칩이 선택한 값. `null`이면 전체.
@riverpod
class SelectedSellerOrderStatus extends _$SelectedSellerOrderStatus {
  @override
  String? build() => null;

  void select(String? status) => state = status;
}

/// 접수/거절/픽업완료/노쇼처리 제출 상태 — 화면이 성공을 감지하면
/// `sellerOrderListProvider(storeId)`를 무효화해서 목록을 새로 그린다.
@riverpod
class SellerOrderActionViewModel extends _$SellerOrderActionViewModel {
  @override
  FutureOr<int?> build() => null; // 성공하면 처리한 orderId를 담아 화면에 알린다.

  Future<void> accept(int orderId) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(orderRepositoryProvider).acceptOrder(orderId);
      return orderId;
    });
  }

  Future<void> reject({required int orderId, required String reason}) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref.read(orderRepositoryProvider).rejectOrder(orderId: orderId, reason: reason);
      return orderId;
    });
  }

  Future<void> updatePickupStatus({required int orderId, required String status}) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      await ref
          .read(orderRepositoryProvider)
          .updatePickupStatus(orderId: orderId, status: status);
      return orderId;
    });
  }
}
