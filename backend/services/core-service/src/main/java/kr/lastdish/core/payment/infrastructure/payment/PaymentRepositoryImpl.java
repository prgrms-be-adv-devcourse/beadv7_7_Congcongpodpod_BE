package kr.lastdish.core.payment.infrastructure.payment;

import java.util.Optional;
import kr.lastdish.core.payment.domain.payment.Payment;
import kr.lastdish.core.payment.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

  private final PaymentJpaRepository paymentJpaRepository;

  @Override
  public Payment save(Payment payment) {
    return paymentJpaRepository.save(payment);
  }

  @Override
  public Optional<Payment> findById(Long id) {
    return paymentJpaRepository.findById(id);
  }

  @Override
  public Optional<Payment> findByMerchantOrderId(String merchantOrderId) {
    return paymentJpaRepository.findByMerchantOrderId(merchantOrderId);
  }

  @Override
  public Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId) {
    return paymentJpaRepository.findWithLockByMerchantOrderId(merchantOrderId);
  }
}
