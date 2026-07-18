package kr.lastdish.core.payment.infrastructure;

import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {

    // 특정 회원의 예치금 내역을 최신순(생성일 기준 내림차순)으로 모두 가져옵니다.
    List<DepositHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
