import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/dish.dart';
import '../../dish/dish_providers.dart';
import '../../store/store_repository_provider.dart';

part 'seller_dish_view_model.g.dart';

/// 매장의 상품(S2). 매장:상품이 1:1(ADR 004)이라 단건 — `GET /stores/{storeId}/dish`
/// (2026-07-27 백엔드 신규, 이슈 #121)로 판매 상태 무관하게 조회한다. 예전엔
/// `GET /dishes?storeId=`(ON_SALE만 반환)를 썼는데 품절/마감 상품이 관리 화면에서
/// 사라지는 문제가 있어서 이걸로 교체함 — store_repository.dart의 `getMyDish` 주석 참고.
@riverpod
Future<Dish?> sellerDish(Ref ref, int storeId) {
  return ref.watch(storeRepositoryProvider).getMyDish(storeId);
}

/// 상품 등록/수정/상태변경 제출 상태 — checkout_view_model.dart와 같은 커맨드형 패턴.
/// 화면이 성공을 감지하면 `sellerDishProvider(storeId)`를 무효화해서 다시 그린다.
@riverpod
class SellerDishActionViewModel extends _$SellerDishActionViewModel {
  @override
  FutureOr<Dish?> build() => null;

  Future<void> create({
    required int storeId,
    required String dishName,
    required String registeredAt,
    String? description,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(dishRepositoryProvider)
          .createDish(
            storeId: storeId,
            dishName: dishName,
            registeredAt: registeredAt,
            description: description,
            stockQuantity: stockQuantity,
            dishPrice: dishPrice,
            discountPrice: discountPrice,
          ),
    );
  }

  /// `AsyncNotifierBase.update(fn)`와 이름이 겹치면 invalid_override가 나서
  /// `updateDish`로 둔다(seller_store_view_model.dart의 `updateStore`와 같은 이유).
  Future<void> updateDish({
    required int dishId,
    required String dishName,
    required String registeredAt,
    String? description,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(dishRepositoryProvider)
          .updateDish(
            dishId: dishId,
            dishName: dishName,
            registeredAt: registeredAt,
            description: description,
            stockQuantity: stockQuantity,
            dishPrice: dishPrice,
            discountPrice: discountPrice,
          ),
    );
  }

  Future<void> changeStatus({required int dishId, required String dishStatus}) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref.read(dishRepositoryProvider).updateDishStatus(
        dishId: dishId,
        dishStatus: dishStatus,
      ),
    );
  }
}
