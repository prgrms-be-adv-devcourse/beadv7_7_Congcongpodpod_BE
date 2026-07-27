/// 주문 상태(백엔드 `OrderStatus` enum, ADR 014로 6개 확정). 서버는 이 값 이름
/// 그대로(영문 enum명)를 JSON으로 주고받는다 — category.dart와 같은 패턴.
const List<String> orderStatusValues = [
  'RESERVED',
  'PICKUP_READY',
  'PICKED_UP',
  'NO_SHOW',
  'CANCELLED',
  'REJECTED',
];

const Map<String, String> orderStatusDisplayNames = {
  'RESERVED': '예약',
  'PICKUP_READY': '픽업대기',
  'PICKED_UP': '픽업완료',
  'NO_SHOW': '노쇼',
  'CANCELLED': '취소',
  'REJECTED': '거절',
};

String orderStatusLabel(String value) => orderStatusDisplayNames[value] ?? value;
