package kr.lastdish.core.point.application.dto;

import java.math.BigDecimal;
import kr.lastdish.core.point.domain.Point;

public record PointBalanceResponse(BigDecimal balance) {
    public static PointBalanceResponse from(Point point) {
        return new PointBalanceResponse(point.getBalance());
    }
}
