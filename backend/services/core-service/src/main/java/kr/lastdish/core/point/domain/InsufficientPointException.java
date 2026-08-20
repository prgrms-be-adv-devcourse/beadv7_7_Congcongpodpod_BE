package kr.lastdish.core.point.domain;

import java.math.BigDecimal;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;

public class InsufficientPointException extends BusinessException {
  public InsufficientPointException(Long memberId, BigDecimal balance, BigDecimal amount) {
    super(
        ErrorCode.INSUFFICIENT_POINT,
        "포인트 잔액이 부족합니다. memberId=" + memberId + ", balance=" + balance + ", amount=" + amount);
  }
}
