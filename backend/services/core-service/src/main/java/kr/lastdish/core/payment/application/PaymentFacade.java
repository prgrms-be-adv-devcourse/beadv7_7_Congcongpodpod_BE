package kr.lastdish.core.payment.application;

import java.math.BigDecimal;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.payment.application.dto.PaymentApproveResponse;
import kr.lastdish.core.payment.application.dto.PgApprovalResult;
import kr.lastdish.core.payment.application.port.PgPaymentGateway;
import kr.lastdish.core.payment.domain.payment.Payment;
import kr.lastdish.core.payment.domain.payment.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentService paymentService;
  private final DepositFacade depositFacade;
  private final PgPaymentGateway pgPaymentGateway;

  // Toss 통신은 트랜잭션 외부, DB 반영은 트랜잭션 내부에서 처리
  public PaymentApproveResponse approve(
      String paymentKey, String merchantOrderId, BigDecimal requestedAmount) {

    // 조회 + 금액 검증
    Payment readyPayment = paymentService.getReadyPayment(merchantOrderId, requestedAmount);

    // Toss와 통신
    PgApprovalResult pgResult =
        pgPaymentGateway.approve(paymentKey, merchantOrderId, requestedAmount);

    // Toss 결제 거절 -> 실패 반영 후 종료
    if (!pgResult.success()) {
      Payment failedPayment = paymentService.failPayment(readyPayment.getId(), pgResult);
      return PaymentApproveResponse.of(failedPayment, null);
    }

    Payment approvedPayment;

    try {
      // Toss 정상 승인 완료 -> 승인 반영 시도
      approvedPayment = paymentService.approvePayment(readyPayment.getId(), pgResult);
    } catch (Exception e) {
      log.error(
          "CRITICAL: Toss 승인 성공, Payment 상태 반영 실패. merchantOrderId={}, pgTransactionId={}",
          merchantOrderId,
          pgResult.pgTransactionId(),
          e);
      throw new PaymentException(
          CommonErrorCode.INTERNAL_ERROR, "결제 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요.");
    }

    try {
      // Toss 정상 승인완료, Payment APPROVED 반영 완료 -> Deposit charge
      var depositResult =
          depositFacade.charge(
              approvedPayment.getMemberId(), approvedPayment.getId(), approvedPayment.getAmount());
      return PaymentApproveResponse.of(approvedPayment, depositResult.balanceAfter());
    } catch (Exception e) {
      log.error(
          "CRITICAL: Toss 승인 성공, Payment 상태 반영 (APPROVED), 예치금 충전 실패. paymentId={}, merchantOrderId={}",
          approvedPayment.getId(),
          merchantOrderId,
          e);
      throw new PaymentException(
          CommonErrorCode.INTERNAL_ERROR, "결제 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요.");
    }
  }
}
