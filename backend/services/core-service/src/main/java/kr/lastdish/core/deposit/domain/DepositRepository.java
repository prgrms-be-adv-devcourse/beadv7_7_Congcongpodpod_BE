package kr.lastdish.core.deposit.domain;

import java.util.Optional;

public interface DepositRepository {
  Deposit save(Deposit deposit);

  Optional<Deposit> findByMemberId(Long memberId);

  Optional<Deposit> findWithLockByMemberId(Long memberId);

  void createDefaultIfAbsent(Long memberId);

  void deleteAll();
}
