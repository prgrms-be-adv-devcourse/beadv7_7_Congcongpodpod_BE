package kr.lastdish.core.payment.domain.deposit;

import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;

public class DepositNotFoundException extends BusinessException {
    public DepositNotFoundException(Long memberId) {
        super(ErrorCode.ENTITY_NOT_FOUND, "예치금 정보를 찾을 수 없습니다. memberId=" + memberId);
    }
}
