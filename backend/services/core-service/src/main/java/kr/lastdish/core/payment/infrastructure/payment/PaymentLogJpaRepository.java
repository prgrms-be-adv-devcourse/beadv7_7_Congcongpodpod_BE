package kr.lastdish.core.payment.infrastructure.payment;

import kr.lastdish.core.payment.domain.payment.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogJpaRepository extends JpaRepository<PaymentLog, Long> {}
