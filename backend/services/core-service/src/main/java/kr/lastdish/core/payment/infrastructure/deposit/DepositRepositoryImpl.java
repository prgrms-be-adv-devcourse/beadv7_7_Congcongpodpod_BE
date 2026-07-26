package kr.lastdish.core.payment.infrastructure.deposit;

import java.util.Optional;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import kr.lastdish.core.payment.domain.deposit.DepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepositRepositoryImpl implements DepositRepository {

  private final DepositJpaRepository depositJpaRepository;

  @Override
  public Deposit save(Deposit deposit) {
    return depositJpaRepository.save(deposit);
  }

  @Override
  public Optional<Deposit> findByMemberId(Long memberId) {
    return depositJpaRepository.findByMemberId(memberId);
  }

  @Override
  public Optional<Deposit> findWithLockByMemberId(Long memberId) {
    return depositJpaRepository.findWithLockByMemberId(memberId);
  }

  @Override
  public void deleteAll() {
    depositJpaRepository.deleteAll();
  }
}
