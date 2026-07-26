package kr.lastdish.core.payment.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByMerchantOrderId(String merchantOrderId);
    Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);
}