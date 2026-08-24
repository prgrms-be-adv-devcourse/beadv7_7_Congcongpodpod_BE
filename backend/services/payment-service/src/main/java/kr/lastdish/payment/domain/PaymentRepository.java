package kr.lastdish.payment.domain;

import java.util.Optional;

public interface PaymentRepository {
  Payment save(Payment payment);

  Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);

  Optional<Payment> findWithLockById(Long id);
}
