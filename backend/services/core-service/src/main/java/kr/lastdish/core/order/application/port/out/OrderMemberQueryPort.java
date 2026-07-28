package kr.lastdish.core.order.application.port.out;

import kr.lastdish.core.order.application.dto.OrderMemberInfo;

public interface OrderMemberQueryPort {

  OrderMemberInfo getOrderMemberInfo(Long memberId);
}
