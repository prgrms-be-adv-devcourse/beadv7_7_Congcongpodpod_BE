package kr.lastdish.core.deposit.infrastructure;

import kr.lastdish.core.deposit.domain.DepositHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositHistoryJpaRepository extends JpaRepository<DepositHistory, Long> {

  Page<DepositHistory> findByMemberId(Long memberId, Pageable pageable);
}
