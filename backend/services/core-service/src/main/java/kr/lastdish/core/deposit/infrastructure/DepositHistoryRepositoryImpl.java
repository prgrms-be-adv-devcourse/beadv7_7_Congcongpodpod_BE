package kr.lastdish.core.deposit.infrastructure;

import java.util.List;
import kr.lastdish.core.deposit.domain.DepositHistory;
import kr.lastdish.core.deposit.domain.DepositHistoryRepository;
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

  @Override
  public List<DepositHistory> findAll() {
    return depositHistoryJpaRepository.findAll(); // 추가
  }

  @Override
  public void deleteAll() {
    depositHistoryJpaRepository.deleteAll();
  }
}
