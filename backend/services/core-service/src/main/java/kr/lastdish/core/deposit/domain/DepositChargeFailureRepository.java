package kr.lastdish.core.deposit.domain;

import java.util.Optional;

public interface DepositChargeFailureRepository {
  DepositChargeFailure save(DepositChargeFailure depositChargeFailure);

  Optional<DepositChargeFailure> findById(Long id);
}
