package kr.lastdish.core.settlement.application;

import kr.lastdish.core.order.application.OrderFacade;
import kr.lastdish.core.order.presentation.dto.OrderSettlementInfo;
import kr.lastdish.core.settlement.application.dto.SettlementOrderData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementOrderReaderAdaptor implements SettlementOrderReader{
    private final OrderFacade orderFacade;

    @Override
    public List<SettlementOrderData> readSettlementOrders(Long storeId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        return orderFacade.findSettlementOrders(storeId, periodStart, periodEnd)
                .stream().map(this::toSettlementOrderData).toList();
    }
    private SettlementOrderData toSettlementOrderData(OrderSettlementInfo order){
        return new SettlementOrderData(
                order.orderId(),
                order.storeId(),
                order.salesAmount().longValueExact(),
                order.orderCompletedAt()
        );
    }
}
