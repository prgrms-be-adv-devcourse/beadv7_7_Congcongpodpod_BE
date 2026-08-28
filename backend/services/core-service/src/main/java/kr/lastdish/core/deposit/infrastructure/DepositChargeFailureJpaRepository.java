package kr.lastdish.core.deposit.infrastructure;

import kr.lastdish.core.deposit.domain.DepositChargeFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositChargeFailureJpaRepository
    extends JpaRepository<DepositChargeFailure, Long> {}
