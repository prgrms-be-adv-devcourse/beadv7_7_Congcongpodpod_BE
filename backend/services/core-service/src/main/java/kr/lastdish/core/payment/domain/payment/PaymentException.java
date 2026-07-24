package kr.lastdish.core.payment.domain.payment;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.ErrorCodeSpec;

public class PaymentException extends BusinessException {

  public PaymentException(ErrorCodeSpec errorCode, String message) { // ErrorCode -> ErrorCodeSpec
    super(errorCode, message);
  }

  public PaymentException(ErrorCodeSpec errorCode) {
    super(errorCode);
  }
}
