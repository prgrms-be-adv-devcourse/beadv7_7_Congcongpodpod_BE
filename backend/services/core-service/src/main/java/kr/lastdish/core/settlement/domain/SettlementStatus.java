package kr.lastdish.core.settlement.domain;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;

public enum SettlementStatus {
  ACCUMULATING {
    @Override
    public SettlementStatus startProcessing() {
      return PROCESSING;
    }
  },
  PROCESSING {
    @Override
    public SettlementStatus complete() {
      return COMPLETED;
    }

    @Override
    public SettlementStatus fail() {
      return FAILED;
    }
  },
  COMPLETED,
  FAILED {
    @Override
    public SettlementStatus restart() {
      return PROCESSING;
    }
  };

  public SettlementStatus complete() {
    throw invalidTransition("완료");
  }

  public SettlementStatus startProcessing() {
    throw invalidTransition("처리 시작");
  }

  public SettlementStatus fail() {
    throw invalidTransition("실패");
  }

  public SettlementStatus restart() {
    throw invalidTransition("재시작");
  }

  private BusinessException invalidTransition(String action) {
    return new BusinessException(
        CommonErrorCode.INVALID_STATE, "%s 상태에서는 정산을 %s 처리할 수 없습니다.".formatted(this, action));
  }
}
