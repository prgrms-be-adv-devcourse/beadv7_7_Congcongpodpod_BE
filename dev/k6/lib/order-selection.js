// 구매자 VU와 연결하지 않고 매장에 들어온 새 RESERVED 주문을 오래된 순서로 고른다.
export function selectOldestNewReservedOrder(rows, minimumOrderId) {
  return (rows || [])
    .filter(
      (order) =>
        order && order.status === 'RESERVED' && Number(order.orderId) > Number(minimumOrderId),
    )
    .sort((left, right) => Number(left.orderId) - Number(right.orderId))[0] || null;
}
