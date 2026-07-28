/// 매장 거절 사유(백엔드 `OrderRejectReason` enum, ADR 012로 확정). order_status.dart와
/// 같은 패턴 — 서버는 영문 enum명 그대로 주고받고, 한글 라벨은 프론트에서 매핑한다.
/// 구매자 취소(`cancelReason`, 자유 텍스트)와는 무관 — 이건 매장 거절(S3) 전용이다.
const List<String> orderRejectReasonValues = [
  'OUT_OF_STOCK',
  'QUALITY_ISSUE',
  'NOT_READY',
  'STORE_CLOSED',
  'SYSTEM_ERROR',
];

const Map<String, String> orderRejectReasonDisplayNames = {
  'OUT_OF_STOCK': '재고 소진',
  'QUALITY_ISSUE': '품질 문제',
  'NOT_READY': '준비 지연',
  'STORE_CLOSED': '매장 휴무',
  'SYSTEM_ERROR': '시스템 오류',
};

String orderRejectReasonLabel(String value) =>
    orderRejectReasonDisplayNames[value] ?? value;
