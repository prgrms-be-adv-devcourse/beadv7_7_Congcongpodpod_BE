package kr.lastdish.core.order.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderSettlementProjection {

    Long getId();

    Long getStoreId();

    BigDecimal getTotalPrice();

    LocalDateTime getUpdatedAt();
}
