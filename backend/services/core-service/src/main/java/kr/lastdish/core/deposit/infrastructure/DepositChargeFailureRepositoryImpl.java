package kr.lastdish.core.deposit.infrastructure;

import java.util.Optional;
import kr.lastdish.core.deposit.domain.DepositChargeFailure;
import kr.lastdish.core.deposit.domain.DepositChargeFailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepositChargeFailureRepositoryImpl implements DepositChargeFailureRepository {

  private final DepositChargeFailureJpaRepository jpaRepository;

  @Override
  public DepositChargeFailure save(DepositChargeFailure depositChargeFailure) {
    return jpaRepository.save(depositChargeFailure);
  }

  @Override
  public Optional<DepositChargeFailure> findById(Long id) {
    return jpaRepository.findById(id);
  }
}
