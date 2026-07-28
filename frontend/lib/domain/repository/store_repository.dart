import '../model/store.dart';

/// 매장 조회 기능 계약. 실제 dio 호출은 lib/data/repository/store_repository_impl.dart가 담당한다.
/// (auth_repository.dart와 같은 원칙 — presentation은 이 인터페이스만 알면 된다.)
abstract interface class StoreRepository {
  /// 위치 기반 주변 매장 목록 조회 (`GET /stores/nearby`).
  /// 위/경도는 지금은 화면(store_list_view_model.dart)에서 하드코딩된 값을 그대로 넘긴다 —
  /// 실기기 위치 연동은 별도 작업이다.
  Future<List<Store>> getNearbyStores({
    required double latitude,
    required double longitude,
    double radiusKm = 3,
    int page = 0,
    int size = 10,
  });

  /// 매장 단건 상세 조회 (`GET /stores/{storeId}`).
  Future<Store> getStoreDetail(int storeId);
}
