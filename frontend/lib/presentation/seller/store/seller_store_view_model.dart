import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../domain/model/store.dart';
import '../../store/store_repository_provider.dart';

part 'seller_store_view_model.g.dart';

/// 매장 단건 조회 — 이미 매장이 있는 셀러가 S1 화면에 들어왔을 때 수정 폼을
/// 미리 채우는 용도 (order_detail_view_model.dart와 같은 family 패턴).
@riverpod
Future<Store> sellerStoreDetail(Ref ref, int storeId) {
  return ref.watch(storeRepositoryProvider).getStoreDetail(storeId);
}

/// S1(매장 등록/수정) 제출 상태. checkout_view_model.dart와 같은 패턴 —
/// 성공하면 결과 [Store]를 그대로 들고 있는다(등록 직후 storeId를 캐싱해야 해서).
@riverpod
class SellerStoreViewModel extends _$SellerStoreViewModel {
  @override
  FutureOr<Store?> build() => null;

  Future<void> register({
    required String storeName,
    required String businessNumber,
    required String storeAddress,
    required String storePhone,
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(storeRepositoryProvider)
          .registerStore(
            storeName: storeName,
            businessNumber: businessNumber,
            storeAddress: storeAddress,
            storePhone: storePhone,
            openTime: openTime,
            closeTime: closeTime,
            latitude: latitude,
            longitude: longitude,
            category: category,
          ),
    );
  }

  /// 이름을 `update`가 아니라 `updateStore`로 둔 이유: `AsyncNotifierBase`에
  /// 이미 `update(fn)`(내장 상태 변경 메서드)가 있어서 같은 이름으로 재정의하면
  /// 시그니처가 달라 컴파일 에러(invalid_override)가 난다.
  Future<void> updateStore({
    required int storeId,
    required String storeName,
    required String storeAddress,
    required String storePhone,
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  }) async {
    state = const AsyncLoading();
    state = await AsyncValue.guard(
      () => ref
          .read(storeRepositoryProvider)
          .updateStore(
            storeId: storeId,
            storeName: storeName,
            storeAddress: storeAddress,
            storePhone: storePhone,
            openTime: openTime,
            closeTime: closeTime,
            latitude: latitude,
            longitude: longitude,
            category: category,
          ),
    );
  }
}
