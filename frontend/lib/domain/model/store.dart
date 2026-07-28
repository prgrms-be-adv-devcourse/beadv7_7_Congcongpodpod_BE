import 'package:freezed_annotation/freezed_annotation.dart';

import 'dish.dart';

part 'store.freezed.dart';
part 'store.g.dart';

/// 매장 정보. 백엔드 `NearbyStoreResponse`(`GET /stores/nearby` 목록 원소)와 대응 —
/// 단건 조회(`GET /stores/{storeId}`)의 `StoreResponse`엔 `dishes` 필드가 없지만,
/// 홈 화면(B3)은 목록만 쓰므로 지금은 하나의 모델로 같이 쓴다.
///
/// ⚠️ 정확한 필드명은 아직 실제 서버에 붙여보지 않아 확정이 아니다 — 여기 적은 필드는
/// 요청 DTO(StoreCreateRequest 등)와 ERD(Stores 테이블) 기준으로
/// "이럴 것이다"라고 추론한 값이다. 실제 연동 시 Swagger 응답과 다르면(특히 storeId, status
/// 필드명) 이 파일과 store_repository_impl.dart를 함께 고쳐야 한다.
///
/// freezed가 불변 클래스 + copyWith + == 를 만들어주고, json_serializable이 fromJson을 만들어준다.
/// 이 파일을 고치면 `dart run build_runner build --delete-conflicting-outputs`를 다시 돌려야
/// store.freezed.dart / store.g.dart가 최신화된다(token_response.dart와 같은 패턴).
@freezed
class Store with _$Store {
  const factory Store({
    required int storeId,
    required String storeName,
    required String storeAddress,
    required String storePhone,
    // "HH:mm" 문자열 그대로 둔다 — 시간 계산이 필요해지면 그때 DateTime 등으로 파싱.
    required String openTime,
    required String closeTime,
    required double latitude,
    required double longitude,
    // 서버가 안 내려주면 null일 수 있어 nullable + 기본 빈 리스트로 다룬다.
    List<String>? holidays,
    // StoreStatus: OPEN, CLOSED, STOPPED. enum 그대로 두지 않고 String으로
    // 받아 화면에서 필요할 때 매핑 — Dish의 dishStatus도 문자열로 내려온다는 점과 일관되게.
    required String status,
    // Category enum(15개, CHICKEN/KOREAN/... — category.dart의 categoryDisplayName 참고).
    // ADR 017(백엔드)로 Dish가 아니라 Store 소유가 됨 — 그래서 여기 있다.
    required String category,
    // 판매중(ON_SALE) 상품만 내려온다 — 서버가 필터링해서 줌(ADR 018).
    // 매장에 지금 판매중인 상품이 없으면 빈 리스트로 온다(품절/마감 매장 등).
    @Default(<Dish>[]) List<Dish> dishes,
  }) = _Store;

  factory Store.fromJson(Map<String, Object?> json) => _$StoreFromJson(json);
}
