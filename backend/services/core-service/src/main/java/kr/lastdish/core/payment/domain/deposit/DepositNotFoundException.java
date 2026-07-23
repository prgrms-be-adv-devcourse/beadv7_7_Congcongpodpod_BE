package kr.lastdish.core.payment.domain.deposit;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;

public class DepositNotFoundException extends BusinessException {
  public DepositNotFoundException(Long memberId) {
    super(CommonErrorCode.ENTITY_NOT_FOUND, "예치금 정보를 찾을 수 없습니다. memberId=" + memberId);
  }
}
