package kr.lastdish.core.payment.infrastructure;

import java.util.List;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {

  Page<DepositHistory> findByMemberId(Long memberId, Pageable pageable);
}
