package kr.lastdish.core.payment.infrastructure.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
    String paymentKey, String orderId, BigDecimal totalAmount, String status, String method, Card card) {
    public record Card(
            String number, //마스킹된 카드번호
            String company //카드사명 또는 코드
    ){}
}
