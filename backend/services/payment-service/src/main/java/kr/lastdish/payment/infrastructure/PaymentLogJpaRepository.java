package kr.lastdish.payment.infrastructure;

import kr.lastdish.payment.domain.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogJpaRepository extends JpaRepository<PaymentLog, Long> {}
