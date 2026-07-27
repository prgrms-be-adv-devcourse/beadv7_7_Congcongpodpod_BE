import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/pickup_code.dart';
import '../order_repository_provider.dart';

part 'order_pickup_view_model.g.dart';

/// 픽업코드 조회. store_detail_view_model.dart와 같은 함수형(family) Provider —
/// 본인 주문이면서 픽업 가능 상태(`PICKUP_READY`)가 아니면 서버가 404를 준다
/// (order_repository.dart 참고) — 화면은 그 에러를 안내 문구로 보여주면 된다.
@riverpod
Future<PickupCode> orderPickupViewModel(Ref ref, int orderId) {
  final repository = ref.watch(orderRepositoryProvider);
  return repository.getPickupCode(orderId);
}
