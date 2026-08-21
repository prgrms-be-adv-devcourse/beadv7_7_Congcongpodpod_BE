package kr.lastdish.payment.infrastructure;

import java.util.Optional;
import kr.lastdish.payment.domain.Payment;
import kr.lastdish.payment.domain.PaymentRepository;
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
  public Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId) {
    return paymentJpaRepository.findWithLockByMerchantOrderId(merchantOrderId);
  }

  @Override
  public Optional<Payment> findWithLockById(Long id) {
    return paymentJpaRepository.findWithLockById(id);
  }
}
