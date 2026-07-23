package kr.lastdish.core.common.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ErrorCodeSpec {
  SOLD_OUT(HttpStatus.CONFLICT, "C004", "재고가 소진되었습니다."),
  DISH_NOT_ON_SALE(HttpStatus.CONFLICT, "D001", "판매중인 상품이 아닙니다."),
  DISH_NOT_FOUND(HttpStatus.NOT_FOUND, "D002", "상품을 찾을 수 없습니다."),
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "D003", "재고가 부족합니다."),
  INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "D004", "재고 수량은 0보다 커야 합니다."),
  DISH_INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "D005", "할인율은 30% 이상이어야 합니다."),
  INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "DEP001", "예치금 잔액이 부족합니다."),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD001", "주문을 찾을 수 없습니다."),
  ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD002", "주문을 취소할 권한이 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
