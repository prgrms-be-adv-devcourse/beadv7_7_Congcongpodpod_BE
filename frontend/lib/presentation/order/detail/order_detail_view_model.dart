import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/order.dart';
import '../order_repository_provider.dart';

part 'order_detail_view_model.g.dart';

/// 주문 단건 조회. store_detail_view_model.dart와 같은 이유로 함수형(family) Provider —
/// orderId 하나로 결과가 정해지는 순수 조회라 커맨드형 Notifier가 필요 없다.
@riverpod
Future<Order> orderDetailViewModel(Ref ref, int orderId) {
  final repository = ref.watch(orderRepositoryProvider);
  return repository.getOrder(orderId);
}
