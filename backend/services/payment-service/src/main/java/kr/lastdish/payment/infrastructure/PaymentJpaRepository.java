package kr.lastdish.payment.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
  // 결제 승인(approve) 시점에 동시 요청을 막기 위한 락 걸린 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findWithLockById(Long id);
}
