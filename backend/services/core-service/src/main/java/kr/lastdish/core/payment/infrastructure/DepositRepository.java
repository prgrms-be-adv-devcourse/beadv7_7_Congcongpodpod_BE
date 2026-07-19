package kr.lastdish.core.payment.infrastructure;

import java.util.Optional;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

  Optional<Deposit> findByMemberId(Long memberId);
}
