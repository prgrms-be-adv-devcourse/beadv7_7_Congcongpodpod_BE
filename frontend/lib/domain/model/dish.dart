import 'package:freezed_annotation/freezed_annotation.dart';

part 'dish.freezed.dart';
part 'dish.g.dart';

/// 상품(서프라이즈백) 정보. 백엔드 `StoreDishResponse`(nearby 임베딩용, `DishResponse`에서
/// storeId/dishStatus를 뺀 축소 뷰)와 대응 — `GET /stores/nearby`의 `dishes[]`에만 쓰인다.
///
/// `dishes[]`엔 판매중(`ON_SALE`) 상품만 내려온다(백엔드 `DishFacade.getOnSaleDishesByStoreId`가
/// 이미 필터링) — 그래서 이 모델엔 판매상태 필드 자체가 없다. 품절/마감 매장은 `dishes`가
/// 그냥 빈 리스트로 온다.
///
/// 가격 필드(`dishPrice`/`discountPrice`)는 백엔드가 `BigDecimal`이라 `10000.00`처럼 소수점을
/// 붙여 내려준다 — `int`로 받으면 파싱 시 캐스팅 에러가 나서 `num`으로 받는다.
@freezed
class Dish with _$Dish {
  const factory Dish({
    required int dishId,
    required String dishName,
    required String registeredAt,
    String? description,
    String? thumbnailUrl,
    required int stockQuantity,
    required num dishPrice,
    required num discountPrice,
    // `GET /stores/nearby`가 임베딩하는 StoreDishResponse엔 없고, `GET /dishes/{dishId}`
    // 단건 조회(전체 DishResponse)에만 있다 — 주문 생성(POST /orders)에 storeId가
    // 필요해서(checkout_screen.dart), 단건 조회로 다시 가져올 때만 채워진다.
    int? storeId,
    // 판매상태(ON_SALE/SOLD_OUT/CLOSED/EXPIRED). storeId와 같은 이유로 nullable —
    // `GET /stores/nearby`(StoreDishResponse)엔 없고, 셀러 상품관리 화면(S2, `getEachStoreDishes`)
    // 응답엔 있다(둘 다 DishResponse를 축소/그대로 쓰는 차이).
    String? dishStatus,
  }) = _Dish;

  factory Dish.fromJson(Map<String, Object?> json) => _$DishFromJson(json);
}
