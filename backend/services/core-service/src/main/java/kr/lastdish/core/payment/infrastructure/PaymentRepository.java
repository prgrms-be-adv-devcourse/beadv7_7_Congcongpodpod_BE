package kr.lastdish.core.payment.infrastructure;

import java.util.Optional;
import kr.lastdish.core.payment.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  // Toss에 넘기는 가맹점 주문번호(merchantOrderId)로 결제 건 조회 (confirm 시점에 사용)
  Optional<Payment> findByMerchantOrderId(String merchantOrderId);
}
