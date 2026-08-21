package kr.lastdish.core.common.exception;

import kr.lastdish.common.api.exception.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ErrorCodeSpec {
  SOLD_OUT(HttpStatus.CONFLICT, "CT004", "재고가 소진되었습니다."),
  CART_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CT005", "장바구니에 접근할 권한이 없습니다."),
  DISH_NOT_ON_SALE(HttpStatus.CONFLICT, "D001", "판매중인 상품이 아닙니다."),
  DISH_NOT_FOUND(HttpStatus.NOT_FOUND, "D002", "상품을 찾을 수 없습니다."),
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "D003", "재고가 부족합니다."),
  INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "D004", "재고 수량은 0보다 커야 합니다."),
  DISH_INVALID_DISCOUNT_RATE(HttpStatus.BAD_REQUEST, "D005", "할인율은 30% 이상이어야 합니다."),
  DISH_ALREADY_EXISTS(HttpStatus.CONFLICT, "D006", "한 개의 상품만 등록이 가능합니다."),
  DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS(
      HttpStatus.BAD_REQUEST, "D007", "픽업 시간은 매장 영업시간 안에 있어야 합니다."),
  INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "DEP001", "예치금 잔액이 부족합니다."),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD001", "주문을 찾을 수 없습니다."),
  ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORD002", "주문을 취소할 권한이 없습니다."),
  PICKUP_CODE_EXISTS(HttpStatus.CONFLICT, "ORD003", "사용중인 픽업 코드입니다."),
  ORDER_NOT_SELLER(HttpStatus.FORBIDDEN, "ORD004", "주문을 접수할 권한이 없습니다."),
  PICKUP_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORD005", "픽업 코드 생성에 실패했습니다."),
  CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD006", "주문할 장바구니 상품을 찾을 수 없습니다."),
  CART_ITEM_NOT_ORDERABLE(HttpStatus.CONFLICT, "ORD007", "현재 주문할 수 없는 장바구니 상품입니다."),
  ORDER_PICKUP_TIME_NOT_ENDED(HttpStatus.CONFLICT, "ORD008", "픽업 종료 시간 이후에 노쇼 처리할 수 있습니다."),
  INVALID_ORDER_PAYMENT_STATUS(HttpStatus.CONFLICT, "ORD009", "결제 대기 상태에서만 결제 완료 처리할 수 있습니다."),
  ORDER_STORE_CLOSED(HttpStatus.CONFLICT, "ORD010", "매장이 영업 중이 아닙니다."),
  ORDER_PICKUP_DEADLINE_PASSED(HttpStatus.CONFLICT, "ORD011", "상품의 픽업 마감 시간이 지났습니다."),
  ORDER_DISH_PRICE_CHANGED(HttpStatus.CONFLICT, "ORD012", "상품 가격이 변경되었습니다. 다시 확인해 주세요."),
  INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "PT001", "포인트 잔액이 부족합니다."),
  IMAGE_UPLOAD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "IMG001", "이미지를 업로드할 권한이 없습니다."),
  UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "IMG002", "지원하지 않는 이미지 형식입니다."),
  INVALID_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "IMG003", "이미지 파일 크기가 허용 범위를 벗어났습니다."),
  INVALID_IMAGE_FILE_NAME(HttpStatus.BAD_REQUEST, "IMG004", "이미지 파일명이 올바르지 않습니다."),
  PRESIGNED_UPLOAD_NOT_FOUND(HttpStatus.NOT_FOUND, "IMG005", "이미지 업로드 발급 이력을 찾을 수 없습니다."),
  PRESIGNED_UPLOAD_INVALID_STATE(HttpStatus.CONFLICT, "IMG006", "이미지 업로드를 확정할 수 없는 상태입니다."),
  IMAGE_METADATA_MISMATCH(HttpStatus.BAD_REQUEST, "IMG007", "업로드된 이미지 정보가 발급 이력과 다릅니다."),
  IMAGE_OBJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "IMG008", "업로드된 이미지를 찾을 수 없습니다."),
  IMAGE_STORAGE_ERROR(HttpStatus.BAD_GATEWAY, "IMG009", "이미지 저장소 처리에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
