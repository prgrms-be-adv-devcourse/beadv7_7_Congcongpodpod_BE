package kr.lastdish.core.payment.application.payment.port;

import java.math.BigDecimal;
import kr.lastdish.core.payment.application.payment.dto.PgApprovalResult;

public interface PgPaymentGateway {

  // PG사에 결제 승인 요청
  PgApprovalResult approve(Long paymentId, String paymentKey, String orderId, BigDecimal amount);
}
