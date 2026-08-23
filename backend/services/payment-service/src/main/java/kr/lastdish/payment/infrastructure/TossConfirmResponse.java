package kr.lastdish.payment.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
    String paymentKey,
    String orderId,
    BigDecimal totalAmount,
    String status,
    String method,
    Card card) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Card(
      String number, // 마스킹된 카드번호
      String issuerCode // 카드사 코드
      ) {}
}
