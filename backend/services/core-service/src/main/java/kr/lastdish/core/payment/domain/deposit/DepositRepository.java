package kr.lastdish.core.payment.domain.deposit;

import java.util.Optional;

public interface DepositRepository {
  Deposit save(Deposit deposit);

  Optional<Deposit> findByMemberId(Long memberId);

  Optional<Deposit> findWithLockByMemberId(Long memberId);

  void deleteAll();
}
