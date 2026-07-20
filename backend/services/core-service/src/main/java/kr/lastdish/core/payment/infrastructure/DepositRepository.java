package kr.lastdish.core.payment.infrastructure;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

  Optional<Deposit> findByMemberId(Long memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Deposit> findWithLockByMemberId(Long memberId);
}
