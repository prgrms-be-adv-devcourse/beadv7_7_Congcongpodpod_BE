/// 상품 판매상태(백엔드 `DishStatus` enum). order_status.dart/category.dart와 같은 패턴.
const List<String> dishStatusValues = ['ON_SALE', 'SOLD_OUT', 'CLOSED', 'EXPIRED'];

const Map<String, String> dishStatusDisplayNames = {
  'ON_SALE': '판매중',
  'SOLD_OUT': '품절',
  'CLOSED': '마감',
  'EXPIRED': '만료',
};

String dishStatusLabel(String value) => dishStatusDisplayNames[value] ?? value;
