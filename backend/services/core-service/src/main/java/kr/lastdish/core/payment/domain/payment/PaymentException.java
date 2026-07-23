package kr.lastdish.core.payment.domain.payment;

import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;

public class PaymentException extends BusinessException {

  public PaymentException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public PaymentException(ErrorCode errorCode) {
    super(errorCode);
  }
}
