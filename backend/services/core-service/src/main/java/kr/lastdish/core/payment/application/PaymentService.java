package kr.lastdish.core.payment.application;

import java.util.UUID;
import kr.lastdish.core.payment.application.dto.PaymentReadyRequest;
import kr.lastdish.core.payment.application.dto.PaymentReadyResponse;
import kr.lastdish.core.payment.domain.payment.Payment;
import kr.lastdish.core.payment.infrastructure.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  @Value("${toss.client-key:test_ck_placeholder}")
  private String tossClientKey;

  @Transactional
  public PaymentReadyResponse readyPayment(Long memberId, PaymentReadyRequest request) {
    String merchantOrderId = UUID.randomUUID().toString();

    Payment payment =
        Payment.ready(memberId, request.amount(), request.pgProvider(), merchantOrderId);

    Payment savedPayment = paymentRepository.save(payment);

    return PaymentReadyResponse.of(savedPayment, tossClientKey);
  }
}
