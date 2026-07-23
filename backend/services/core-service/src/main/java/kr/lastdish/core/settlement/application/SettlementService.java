package kr.lastdish.core.settlement.application;

import kr.lastdish.core.settlement.application.dto.SettlementOrderData;
import kr.lastdish.core.settlement.domain.SettlementDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private final SettlementOrderReader settlementOrderReader;
    private final SettlementDetailRepository settlementDetailRepository;

    public List<SettlementOrderData> findUnsettledOrders(
            Long storeId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        List<SettlementOrderData> orders =
                settlementOrderReader.readSettlementOrders(
                        storeId,
                        periodStart,
                        periodEnd
                );

        if (orders.isEmpty()) {
            return List.of();
        }

        Set<Long> settledOrderIds =
                settlementDetailRepository.findSettledOrderIds(
                        orders.stream()
                                .map(SettlementOrderData::orderId)
                                .toList()
                );

        return orders.stream()
                .filter(order ->
                        !settledOrderIds.contains(order.orderId())
                )
                .toList();
    }
}
