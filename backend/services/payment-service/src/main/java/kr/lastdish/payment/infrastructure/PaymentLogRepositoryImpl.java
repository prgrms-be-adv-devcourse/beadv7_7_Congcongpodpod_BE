package kr.lastdish.payment.infrastructure;

import kr.lastdish.payment.domain.PaymentLog;
import kr.lastdish.payment.domain.PaymentLogRepository;
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
