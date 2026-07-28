package kr.lastdish.core.payment.infrastructure.payment;

import kr.lastdish.core.payment.domain.payment.PaymentLog;
import kr.lastdish.core.payment.domain.payment.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentLogRepositoryImpl implements PaymentLogRepository {

  private final PaymentLogJpaRepository paymentLogJpaRepository;

  @Override
  public PaymentLog save(PaymentLog paymentLog) {
    return paymentLogJpaRepository.save(paymentLog);
  }
}
