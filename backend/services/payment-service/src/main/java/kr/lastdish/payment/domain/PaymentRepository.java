package kr.lastdish.payment.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository {
  Payment save(Payment payment);

  Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);

  Optional<Payment> findWithLockById(Long id);

  int expireReadyStatePayments(LocalDateTime now, LocalDateTime threshold, int batchSize);
}
