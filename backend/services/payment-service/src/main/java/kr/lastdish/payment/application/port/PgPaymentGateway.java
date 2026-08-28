package kr.lastdish.payment.application.port;

import java.math.BigDecimal;
import kr.lastdish.payment.application.dto.PgApprovalResult;

public interface PgPaymentGateway {

  // PG사에 결제 승인 요청
  PgApprovalResult approve(Long paymentId, String paymentKey, String orderId, BigDecimal amount);

  PgApprovalResult checkStatus(String merchantOrderId);
}
