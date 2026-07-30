/// 픽업 시간 범위를 화면 표시용으로 포맷한다.
///
/// 백엔드가 Dish 등록 시 픽업시간을 저장하는 코드 경로가 없어(2026-07-30 발견,
/// `Dish.create()` 확인) 지금 생성되는 모든 주문의 픽업시간이 항상 null로 내려온다.
/// null이면 "HH:mm" 자르기(`substring`)를 시도하는 대신 안내 문구로 대체한다.
String formatPickupWindow(String? startAt, String? endAt) {
  if (startAt == null || endAt == null) return '픽업 시간 미정';
  return '${startAt.substring(0, 5)} ~ ${endAt.substring(0, 5)}';
}
