package kr.lastdish.core.deposit.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepositHistoryRepository {
  DepositHistory save(DepositHistory depositHistory);

  Page<DepositHistory> findByMemberId(Long memberId, Pageable pageable);

  List<DepositHistory> findAll();

  void deleteAll();
}
