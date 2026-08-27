package kr.lastdish.payment.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
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

  @Override
  public int expireReadyStatePayments(LocalDateTime now, LocalDateTime threshold, int batchSize) {
    return paymentJpaRepository.expireReadyStatePayments(now, threshold, batchSize);
  }

  @Override
  public int claimProcessingPayments(
          LocalDateTime now, LocalDateTime threshold, LocalDateTime lockTimeout, int batchSize) {
    return paymentJpaRepository.claimProcessingPayments(now, threshold, lockTimeout, batchSize);
  }

  @Override
  public List<Payment> findClaimedProcessingPayments(LocalDateTime now) {
    return paymentJpaRepository.findClaimedProcessingPayments(now);
  }
}
