// 구매자 VU와 연결하지 않고 매장에 들어온 새 RESERVED 주문을 오래된 순서로 고른다.
export function selectOldestNewReservedOrder(rows, minimumOrderId) {
  return (rows || [])
    .filter(
      (order) =>
        order && order.status === 'RESERVED' && Number(order.orderId) > Number(minimumOrderId),
    )
    .sort((left, right) => Number(left.orderId) - Number(right.orderId))[0] || null;
}

/**
 * 픽업할 주문을 고른다 — **이번 반복에서 수락한 것은 제외한다.**
 *
 * 실제 서비스에서 PICKUP_READY는 판매자가 수락한 뒤 손님이 매장에 올 때까지 유지되므로
 * 분~시간 단위로 존재한다. 그런데 부하 시나리오는 수락과 픽업을 한 반복 안에서 연달아
 * 처리해 그 상태가 수 초만 존재했고, 구매자의 픽업코드 조회가 전부 실패했다
 * (2026-08-29 실측: order_pickup_codes_batch 0% — 31건 전부).
 *
 * 방금 수락한 주문을 빼고 가장 오래된 것을 고르면, 주문은 최소 한 반복을 PICKUP_READY로
 * 머문다. 수락과 픽업 처리량은 그대로 유지하면서 상태만 실제에 가까워진다.
 */
export function selectOldestPickupReadyOrder(rows, minimumOrderId, excludeOrderId) {
  return (
    (rows || [])
      .filter(
        (order) =>
          order &&
          order.status === 'PICKUP_READY' &&
          Number(order.orderId) > Number(minimumOrderId) &&
          Number(order.orderId) !== Number(excludeOrderId),
      )
      .sort((left, right) => Number(left.orderId) - Number(right.orderId))[0] || null
  );
}
