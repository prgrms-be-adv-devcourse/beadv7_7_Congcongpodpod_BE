package kr.lastdish.core.payment.domain.deposit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepositHistoryRepository {
    DepositHistory save(DepositHistory depositHistory);

    Page<DepositHistory> findByMemberId(Long memberId, Pageable pageable);
}
