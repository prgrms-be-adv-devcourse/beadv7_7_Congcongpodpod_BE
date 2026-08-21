package kr.lastdish.payment.application;

import java.math.BigDecimal;
import java.util.UUID;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.payment.application.dto.ApprovalClaim;
import kr.lastdish.payment.application.dto.PaymentReadyRequest;
import kr.lastdish.payment.application.dto.PaymentReadyResponse;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.application.event.ChargeRequestedEventWriter;
import kr.lastdish.payment.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentLogRepository paymentLogRepository;
  private final ChargeRequestedEventWriter chargeRequestedEventWriter;

  @Value("${toss.client-key:test_ck_placeholder}")
  private String tossClientKey;

  // 결제 준비: Payment를 READY 상태로 생성하고, 프론트가 결제위젯을 띄우는 데 필요한 정보를 반환
  @Transactional
  public PaymentReadyResponse readyPayment(Long memberId, PaymentReadyRequest request) {
    String merchantOrderId = UUID.randomUUID().toString();

    Payment payment =
        Payment.ready(memberId, request.amount(), request.pgProvider(), merchantOrderId);

    Payment savedPayment = paymentRepository.save(payment);

    return PaymentReadyResponse.of(savedPayment, tossClientKey);
  }

  // 결제 승인 요청 선점 : merchantOrderId로 락을 걸고 금액 검증
  // READY → CONFIRMING 전환(선점 성공) 또는 ALREADY_PROCESSING(이미 처리 중) 반환
  @Transactional
  public ApprovalClaim claimApproval(String merchantOrderId, BigDecimal requestedAmount) {
    Payment payment =
        paymentRepository
            .findWithLockByMerchantOrderId(merchantOrderId)
            .orElseThrow(
                () ->
                    new PaymentException(
                        CommonErrorCode.ENTITY_NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. merchantOrderId=" + merchantOrderId));
    if (payment.getAmount().compareTo(requestedAmount) != 0) {
      throw new PaymentException(
          CommonErrorCode.INVALID_INPUT,
          "요청 금액이 결제 금액과 일치하지 않습니다. merchantOrderId=" + merchantOrderId);
    }
    ApprovalClaimResult result = payment.claimApproval();
    return new ApprovalClaim(result, payment);
  }

  // 결제 승인 : 결제 성공 결과를 Payment에 반영하고, PaymentLog 테이블에 기록
  @Transactional
  public Payment approvePayment(Long paymentId, PgApprovalResult pgResult) {
    Payment payment =
        paymentRepository
            .findWithLockById(paymentId)
            .orElseThrow(
                () ->
                    new PaymentException(
                        CommonErrorCode.ENTITY_NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));
    payment.approve(pgResult.pgTransactionId());
    paymentLogRepository.save(
        PaymentLog.createResponseLog(
            payment.getId(),
            payment.getPgProvider(),
            pgResult.pgTransactionId(),
            pgResult.paymentMethod(),
            pgResult.maskedCardNumber(),
            pgResult.issuerCode(),
            pgResult.failureCode(),
            pgResult.failureMessage(),
            pgResult.success() ? 200 : 400,
            pgResult.success() ? "DONE" : pgResult.failureCode()));
    chargeRequestedEventWriter.append(payment, payment.nextAggregateVersion());
    return payment;
  }

  // 결제 실패 : 결제 실패 결과를 Payment에 반영하고, PaymentLog 테이블에 기록
  @Transactional
  public Payment failPayment(Long paymentId, PgApprovalResult pgResult) {
    Payment payment =
        paymentRepository
            .findWithLockById(paymentId)
            .orElseThrow(
                () ->
                    new PaymentException(
                        CommonErrorCode.ENTITY_NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId=" + paymentId));
    payment.fail();
    paymentLogRepository.save(
        PaymentLog.createResponseLog(
            payment.getId(),
            payment.getPgProvider(),
            pgResult.pgTransactionId(),
            pgResult.paymentMethod(),
            pgResult.maskedCardNumber(),
            pgResult.issuerCode(),
            pgResult.failureCode(),
            pgResult.failureMessage(),
            pgResult.success() ? 200 : 400,
            pgResult.success() ? "DONE" : pgResult.failureCode()));
    return payment;
  }
}
