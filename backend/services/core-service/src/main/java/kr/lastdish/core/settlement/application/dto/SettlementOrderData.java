package kr.lastdish.core.settlement.application.dto;

import java.time.LocalDateTime;

public record SettlementOrderData(
        Long orderId,
        Long storeId,
        Long salesAmount,
        LocalDateTime orderCompletedAt
) {
    public SettlementOrderData {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }

        if (storeId == null) {
            throw new IllegalArgumentException("매장 ID는 필수입니다.");
        }

        if (salesAmount < 0) {
            throw new IllegalArgumentException(
                    "판매 금액은 음수일 수 없습니다."
            );
        }

        if (orderCompletedAt == null) {
            throw new IllegalArgumentException(
                    "주문 완료 시각은 필수입니다."
            );
        }
    }
}
