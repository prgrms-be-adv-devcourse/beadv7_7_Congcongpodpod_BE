package kr.lastdish.core.payment.application.port;

import java.math.BigDecimal;
import kr.lastdish.core.payment.application.dto.PgApprovalResult;

public interface PgPaymentGateway {

  // PG사에 결제 승인 요청
  PgApprovalResult approve(String paymentKey, String orderId, BigDecimal amount);
}
