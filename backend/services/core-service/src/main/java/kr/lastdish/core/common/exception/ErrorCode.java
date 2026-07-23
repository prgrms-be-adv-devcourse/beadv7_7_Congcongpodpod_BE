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
  DISH_ALREADY_EXISTS(HttpStatus.CONFLICT, "D006", "한 개의 상품만 등록이 가능합니다."),
  INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "DEP001", "예치금 잔액이 부족합니다."),
  INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "PAY001", "결제 대기 상태에서만 처리할 수 있습니다."),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD001", "주문을 찾을 수 없습니다."),
  ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD002", "주문을 취소할 권한이 없습니다."),
  PICKUP_CODE_EXISTS(HttpStatus.CONFLICT, "ORD003", "사용중인 픽업 코드입니다."),
  ORDER_NOT_SELLER(HttpStatus.FORBIDDEN, "ORD004", "주문을 접수할 권한이 없습니다."),
  PICKUP_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORD005", "픽업 코드 생성에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
