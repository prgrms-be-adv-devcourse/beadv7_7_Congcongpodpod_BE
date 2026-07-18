package kr.lastdish.core.payment.infrastructure;

import kr.lastdish.core.payment.domain.deposit.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

    Optional<Deposit> findByMemberId(Long memberId);
}