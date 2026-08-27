package kr.lastdish.payment.application;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.payment.application.dto.ApprovalClaim;
import kr.lastdish.payment.application.dto.PaymentApproveResponse;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.application.port.PgPaymentGateway;
import kr.lastdish.payment.domain.Payment;
import kr.lastdish.payment.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentService paymentService;
  private final PgPaymentGateway pgPaymentGateway;

  @Value("${payment.processing-verify.delay-ms:200}")
  private long verifyDelayMs;

  // Toss 통신은 트랜잭션 외부, DB 반영은 트랜잭션 내부에서 처리
  public PaymentApproveResponse approve(
      String paymentKey, String merchantOrderId, BigDecimal requestedAmount) {

    // 선점: 이 요청이 실제로 PG를 호출할 권리를 가졌는지 확인
    ApprovalClaim claim = paymentService.claimApproval(merchantOrderId, requestedAmount);

    return switch (claim.result()) {
      case STARTED -> confirmWithPg(claim.payment(), paymentKey, merchantOrderId, requestedAmount);
      case ALREADY_PROCESSING -> {
        log.info("이미 처리 중인 결제 요청입니다. merchantOrderId={}", merchantOrderId);
        yield PaymentApproveResponse.of(claim.payment(), "이미 처리 중인 결제 요청입니다. 잠시 후 다시 확인해주세요.");
      }
      case ALREADY_APPROVED -> {
        log.info("이미 승인 완료된 결제에 대한 중복 요청입니다. merchantOrderId={}", merchantOrderId);
        yield PaymentApproveResponse.of(claim.payment(), "이미 예치금 충전이 완료된 결제입니다.");
      }
      case ALREADY_FAILED -> {
        log.info("이미 실패 처리된 결제에 대한 중복 요청입니다. merchantOrderId={}", merchantOrderId);
        yield PaymentApproveResponse.of(claim.payment(), null);
      }
    };
  }

  private PaymentApproveResponse confirmWithPg(
      Payment payment, String paymentKey, String merchantOrderId, BigDecimal requestedAmount) {

    PgApprovalResult pgResult =
        pgPaymentGateway.approve(payment.getId(), paymentKey, merchantOrderId, requestedAmount);

    return switch (pgResult.status()) {
      case FAILURE -> {
        log.warn(
            "Toss 승인 거절: code={}, message={}", pgResult.failureCode(), pgResult.failureMessage());
        Payment failedPayment = paymentService.failPayment(payment.getId(), pgResult);
        yield PaymentApproveResponse.of(failedPayment, null);
      }
      case UNKNOWN -> {
        log.error(
            "결제 결과를 확정할 수 없습니다. merchantOrderId={}, paymentKey={}", merchantOrderId, paymentKey);
        yield PaymentApproveResponse.of(payment, "결제 확인 중입니다. 잠시 후 다시 확인해주세요.");
      }
      case SUCCESS -> {
        try {
          Payment approvedPayment = paymentService.approvePayment(payment.getId(), pgResult);
          yield PaymentApproveResponse.of(approvedPayment, "예치금 충전이 진행 중입니다.");
        } catch (Exception e) {
          log.error(
              "CRITICAL: Toss 승인 성공, Payment 상태 반영 실패. merchantOrderId={}, pgTransactionId={}",
              merchantOrderId,
              pgResult.pgTransactionId(),
              e);
          throw new PaymentException(
              CommonErrorCode.INTERNAL_ERROR, "결제 처리 중 오류가 발생했습니다. 고객센터로 문의해주세요.");
        }
      }
    };
  }

  public void verifyProcessingPayments() {
    List<Payment> claimed = paymentService.claimStuckProcessingPayments();
    if (claimed.isEmpty()) {
      return;
    }

    log.info("PROCESSING 상태 확인 배치 대상 {}건을 선점했습니다.", claimed.size());

    for (Payment payment : claimed) {
      verifyPaymentStatus(payment);
      sleepBetweenVerifications();
    }
  }

  private void verifyPaymentStatus(Payment payment) {
    PgApprovalResult pgResult = pgPaymentGateway.checkStatus(payment.getMerchantOrderId());

    try {
      switch (pgResult.status()) {
        case SUCCESS -> {
          paymentService.approvePayment(payment.getId(), pgResult);
          log.info("결제 상태 확인 배치 : APPROVED로 확정. paymentId={}", payment.getId());
        }
        case FAILURE -> {
          paymentService.failPayment(payment.getId(), pgResult);
          log.info("결제 상태 확인 배치 : FAILED로 확정. paymentId={}", payment.getId());
        }
        case UNKNOWN ->
            log.info(
                "결제 상태 확인 배치 : 확정 보류, 다음 사이클로 넘김. paymentId={}, reason={}",
                payment.getId(),
                pgResult.failureMessage());
      }
    } catch (PaymentException e) {
      log.info(
          "결제 상태 확인 배치 : 이미 다른 경로에서 처리됨. paymentId={}, message={}",
          payment.getId(),
          e.getMessage());
    } catch (Exception e) {
      log.error("결제 상태 확인 배치 : 확정 처리 중 예외, 다음 후보로 넘어갑니다. paymentId={}", payment.getId(), e);
    }
  }

  private void sleepBetweenVerifications() {
    try {
      Thread.sleep(verifyDelayMs);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
