package kr.lastdish.payment.infrastructure;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
  // 결제 승인(approve) 시점에 동시 요청을 막기 위한 락 걸린 조회
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findWithLockByMerchantOrderId(String merchantOrderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findWithLockById(Long id);

  @Modifying(clearAutomatically = true)
  @Query(
          value = """

                  UPDATE payments
          SET approved_status = 'EXPIRED', updated_at = :now
          WHERE id IN (
              SELECT id FROM payments
              WHERE approved_status = 'READY' AND updated_at < :threshold
              ORDER BY updated_at
              LIMIT :batchSize
              FOR UPDATE SKIP LOCKED
          )
          """,
          nativeQuery = true)
  int expireReadyStatePayments(
          @Param("now") LocalDateTime now,
          @Param("threshold") LocalDateTime threshold,
          @Param("batchSize") int batchSize);


  @Modifying
  @Query(
          value = """
        UPDATE payments
        SET locked_at = :now
        WHERE id IN (
            SELECT id FROM payments
            WHERE approved_status = 'PROCESSING'
              AND updated_at < :threshold
              AND (locked_at IS NULL OR locked_at < :lockTimeout)
            ORDER BY updated_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        )
        """,
          nativeQuery = true)
  int claimProcessingPayments(
          @Param("now") LocalDateTime now,
          @Param("threshold") LocalDateTime threshold,
          @Param("lockTimeout") LocalDateTime lockTimeout,
          @Param("batchSize") int batchSize);

  @Query(
          value = "SELECT * FROM payments WHERE approved_status = 'PROCESSING' AND locked_at = :now",
          nativeQuery = true)
  List<Payment> findClaimedProcessingPayments(@Param("now") LocalDateTime now);
  }
