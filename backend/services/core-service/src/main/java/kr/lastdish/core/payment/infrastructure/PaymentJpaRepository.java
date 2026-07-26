package kr.lastdish.core.payment.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.payment.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

  // Toss에 넘기는 가맹점 주문번호(merchantOrderId)로 결제 건 조회
  // 락 없는 단순 조회. 결제 상세 조회에 사용.
  Optional<Payment> findByMerchantOrderId(String merchantOrderId);

  // 결제 승인(approve) 시점에 동시 요청을 막기 위한 락 걸린 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);
}
