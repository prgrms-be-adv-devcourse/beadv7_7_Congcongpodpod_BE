package kr.lastdish.core.payment.domain.deposit;

import java.math.BigDecimal;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;

public class InsufficientBalanceException extends BusinessException {
  public InsufficientBalanceException(Long memberId, BigDecimal balance, BigDecimal requested) {
    super(
        ErrorCode.INSUFFICIENT_BALANCE,
        String.format("잔액이 부족합니다. 회원=%d, 보유=%s, 요청=%s", memberId, balance, requested));
  }
}
