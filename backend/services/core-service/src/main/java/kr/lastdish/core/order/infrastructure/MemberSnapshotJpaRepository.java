package kr.lastdish.core.order.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.lastdish.core.order.domain.MemberSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberSnapshotJpaRepository extends JpaRepository<MemberSnapshot, Long> {

  Optional<MemberSnapshot> findByMemberIdAndDeletedFalse(Long memberId);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO member_snapshots
              (member_id, name, phone, updated_at, aggregate_version, is_deleted)
          VALUES
              (:memberId, :name, :phone, :updatedAt, :aggregateVersion, false)
          ON CONFLICT (member_id) DO UPDATE SET
              name = EXCLUDED.name,
              phone = EXCLUDED.phone,
              updated_at = EXCLUDED.updated_at,
              aggregate_version = EXCLUDED.aggregate_version,
              is_deleted = false
          WHERE EXCLUDED.aggregate_version > member_snapshots.aggregate_version
          """,
      nativeQuery = true)
  int upsertIfNewer(
      @Param("memberId") Long memberId,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("aggregateVersion") long aggregateVersion,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO member_snapshots
              (member_id, name, phone, updated_at, aggregate_version, is_deleted)
          VALUES
              (:memberId, :name, :phone, :updatedAt, :aggregateVersion, true)
          ON CONFLICT (member_id) DO UPDATE SET
              name = EXCLUDED.name,
              phone = EXCLUDED.phone,
              updated_at = EXCLUDED.updated_at,
              aggregate_version = EXCLUDED.aggregate_version,
              is_deleted = true
          WHERE EXCLUDED.aggregate_version > member_snapshots.aggregate_version
          """,
      nativeQuery = true)
  int markDeletedIfNewer(
      @Param("memberId") Long memberId,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("aggregateVersion") long aggregateVersion,
      @Param("updatedAt") LocalDateTime updatedAt);
}
