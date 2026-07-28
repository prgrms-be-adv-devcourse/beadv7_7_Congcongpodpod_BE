import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../core/network/dio_provider.dart';
import '../../data/repository/dish_repository_impl.dart';
import '../../domain/model/dish.dart';
import '../../domain/repository/dish_repository.dart';

part 'dish_providers.g.dart';

/// DishRepository를 조립해 앱 전역에 제공한다 (store_repository_provider.dart와 같은 패턴).
@riverpod
DishRepository dishRepository(Ref ref) {
  return DishRepositoryImpl(dio: ref.watch(dioProvider));
}

/// 상품 단건 조회. family라 `dishId`별로 각각 캐시된다 — 같은 상품을 여러 화면에서
/// 동시에 봐도 dishId가 같으면 API 호출은 한 번만 나간다.
@riverpod
Future<Dish> dish(Ref ref, int dishId) {
  return ref.watch(dishRepositoryProvider).getDish(dishId);
}
