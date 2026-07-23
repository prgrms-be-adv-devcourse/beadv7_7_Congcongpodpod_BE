package kr.lastdish.core.payment.infrastructure;

import kr.lastdish.core.payment.domain.payment.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {}
