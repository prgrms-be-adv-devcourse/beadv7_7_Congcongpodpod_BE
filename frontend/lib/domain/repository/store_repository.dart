import '../model/dish.dart';
import '../model/store.dart';

/// 매장 조회 기능 계약. 실제 dio 호출은 lib/data/repository/store_repository_impl.dart가 담당한다.
/// (auth_repository.dart와 같은 원칙 — presentation은 이 인터페이스만 알면 된다.)
abstract interface class StoreRepository {
  /// 위치 기반 주변 매장 목록 조회 (`GET /stores/nearby`).
  /// 위/경도는 지금은 화면(store_list_view_model.dart)에서 하드코딩된 값을 그대로 넘긴다 —
  /// 실기기 위치 연동은 별도 작업이다.
  /// `category`가 null이면 전체 카테고리(필터 없음) — 서버 쿼리 파라미터 자체를 안 보낸다.
  Future<List<Store>> getNearbyStores({
    required double latitude,
    required double longitude,
    double radiusKm = 3,
    int page = 0,
    int size = 10,
    String? category,
  });

  /// 매장 단건 상세 조회 (`GET /stores/{storeId}`).
  Future<Store> getStoreDetail(int storeId);

  /// 내 매장 목록 (`GET /stores/mine`, SELLER 전용). 2026-07-27 백엔드에 실제
  /// 반영됨(adr-draft-seller-store-id-resolution.md 참고) — S2/S3가 필요로 하는
  /// storeId를 이 API로 직접 얻는다(로컬 캐싱 임시방편은 걷어냄).
  Future<List<Store>> getMyStores();

  /// 매장 등록 (`POST /stores`, SELLER 전용). 성공하면 storeId가 포함된 [Store]를
  /// 돌려준다 — 화면이 이 storeId를 seller_store_id_provider.dart로 캐싱해서
  /// S2/S3에서 재사용한다(adr-draft-seller-store-id-resolution.md 참고, `GET /stores/mine`
  /// API가 나오기 전까지의 임시 방편).
  Future<Store> registerStore({
    required String storeName,
    required String businessNumber,
    required String storeAddress,
    required String storePhone,
    required String openTime, // "HH:mm"
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  });

  /// 매장 정보 수정 (`PUT /stores/{storeId}`, SELLER 전용). `businessNumber`는
  /// 요청에 없다 — 등록 후 불변으로 취급(백엔드 `UpdateStoreRequest`에 그 필드 자체가 없음).
  Future<Store> updateStore({
    required int storeId,
    required String storeName,
    required String storeAddress,
    required String storePhone,
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    required String category,
  });

  /// 내 매장의 상품 단건 조회 (`GET /stores/{storeId}/dish`, SELLER 전용,
  /// 2026-07-27 백엔드 신규 반영, 이슈 #121). 매장:상품 1:1(ADR 004)이라 단건이고,
  /// `GET /dishes?storeId=`와 달리 판매 상태 무관(품절/마감도 나옴) — S2 상품관리
  /// 화면 전용. 상품을 아직 등록 안 했으면(`D002`) null.
  Future<Dish?> getMyDish(int storeId);
}
