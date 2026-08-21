package kr.lastdish.core.deposit.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.deposit.domain.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositJpaRepository extends JpaRepository<Deposit, Long> {

  Optional<Deposit> findByMemberId(Long memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Deposit> findWithLockByMemberId(Long memberId);

  @Modifying
  @Query(
      value =
          "INSERT INTO deposits (member_id, balance, updated_at) "
              + "VALUES (:memberId, 0, now()) "
              + "ON CONFLICT (member_id) DO NOTHING",
      nativeQuery = true)
  void createDefaultIfAbsent(@Param("memberId") Long memberId);
}
