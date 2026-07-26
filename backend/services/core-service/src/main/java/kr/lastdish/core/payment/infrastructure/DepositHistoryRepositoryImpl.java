package kr.lastdish.core.payment.infrastructure;


import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import kr.lastdish.core.payment.domain.deposit.DepositHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepositHistoryRepositoryImpl implements DepositHistoryRepository {

    private final DepositHistoryJpaRepository depositHistoryJpaRepository;

    @Override
    public DepositHistory save(DepositHistory depositHistory) {
        return depositHistoryJpaRepository.save(depositHistory);
    }

    @Override
    public Page<DepositHistory> findByMemberId(Long memberId, Pageable pageable) {
        return depositHistoryJpaRepository.findByMemberId(memberId, pageable);
    }
}