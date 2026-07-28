import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/order.dart';
import '../order_repository_provider.dart';

part 'order_list_view_model.g.dart';

/// 내 주문 목록 화면의 상태. store_list_view_model.dart와 같은 패턴 —
/// `ref.watch(selectedOrderStatusProvider)`로 상태 필터 탭을 구독해서, 탭이 바뀌면
/// 자동으로 다시 조회한다.
@riverpod
class OrderListViewModel extends _$OrderListViewModel {
  @override
  Future<List<Order>> build() {
    final repository = ref.watch(orderRepositoryProvider);
    final status = ref.watch(selectedOrderStatusProvider);
    return repository.getMyOrders(status: status);
  }

  Future<void> refresh() async {
    ref.invalidateSelf();
    await future;
  }
}

/// 주문 목록 상태 필터 탭이 선택한 값. `null`이면 "전체".
@riverpod
class SelectedOrderStatus extends _$SelectedOrderStatus {
  @override
  String? build() => null;

  void select(String? status) => state = status;
}
