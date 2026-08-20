package kr.lastdish.core.deposit.infrastructure;

import java.util.Optional;
import kr.lastdish.core.deposit.domain.Deposit;
import kr.lastdish.core.deposit.domain.DepositRepository;
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
