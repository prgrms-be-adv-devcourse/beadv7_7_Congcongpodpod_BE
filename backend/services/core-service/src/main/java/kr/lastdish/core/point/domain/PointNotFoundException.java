package kr.lastdish.core.point.domain;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;

public class PointNotFoundException extends BusinessException {
  public PointNotFoundException(Long memberId) {
    super(CommonErrorCode.ENTITY_NOT_FOUND, "포인트 정보를 찾을 수 없습니다. memberId=" + memberId);
  }
}