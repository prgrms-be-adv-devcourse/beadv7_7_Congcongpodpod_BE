import '../model/dish.dart';

/// 상품 단건 조회 기능 계약. 실제 dio 호출은 lib/data/repository/dish_repository_impl.dart가 담당한다.
/// (store_repository.dart와 같은 원칙.)
abstract interface class DishRepository {
  /// 상품 단건 조회 (`GET /dishes/{dishId}`). 지금은 장바구니 화면이 "지금 재고가
  /// 몇 개 남았는지" 보여주는 용도로만 쓴다 — `CartItemResponse`엔 재고 정보가 없어서
  /// (dishId만 있음), 필요할 때 이 API로 따로 가져온다.
  Future<Dish> getDish(int dishId);

  /// 매장의 상품 목록 (`GET /dishes?storeId=`, S2 상품관리용).
  /// ⚠️ 알려진 API 제약: 백엔드가 `getOnSaleDishesByStoreId`를 그대로 재사용하고 있어
  /// **ON_SALE 상태 상품만** 돌아온다 — 품절/마감으로 바꾼 상품은 이 목록에서 사라진다.
  /// 셀러용 "전체 상태 조회" API는 아직 없다(2026-07-27 확인, backlog.md 참고). 백엔드에
  /// 별도 API가 생기기 전까지 이 제약을 안고 간다.
  Future<List<Dish>> getDishesByStore(int storeId);

  /// 상품 등록 (`POST /dishes`, SELLER 전용). `registeredAt`은 "yyyy-MM-ddTHH:mm:ss"
  /// (백엔드 `LocalDateTime`, `@JsonFormat` 없이 기본 ISO 포맷을 그대로 받음).
  ///
  /// `pickupStartTime`/`pickupEndTime`("HH:mm")은 그 매장의 영업시간을 그대로 넘긴다 —
  /// 백엔드가 Dish 등록 시 픽업시간을 채우는 로직이 없어서(troubleshooting 참고,
  /// 2026-07-30) 프론트가 매장 조회로 얻은 값을 실어 보낸다. 서버 쪽에서 Store를 직접
  /// 조회하면 store↔dish 컨텍스트 순환 의존이 생기는 걸 피하려는 선택.
  Future<Dish> createDish({
    required int storeId,
    required String dishName,
    required String registeredAt,
    String? description,
    String? thumbnailUrl,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
    String? pickupStartTime,
    String? pickupEndTime,
  });

  /// 상품 수정 (`PUT /dishes/{dishId}`, SELLER 전용).
  Future<Dish> updateDish({
    required int dishId,
    required String dishName,
    required String registeredAt,
    String? description,
    String? thumbnailUrl,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
  });

  /// 상품 삭제(soft) (`PATCH /dishes/{dishId}`, SELLER 전용).
  Future<void> deleteDish(int dishId);

  /// 상품 상태 변경 (`PATCH /dishes/{dishId}/status`, SELLER 전용).
  /// `dishStatus`: `ON_SALE`/`SOLD_OUT`/`CLOSED`/`EXPIRED`.
  Future<Dish> updateDishStatus({required int dishId, required String dishStatus});
}
