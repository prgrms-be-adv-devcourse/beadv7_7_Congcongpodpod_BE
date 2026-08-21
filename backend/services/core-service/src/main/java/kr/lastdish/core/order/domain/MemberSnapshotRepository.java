package kr.lastdish.core.order.domain;

import java.util.Optional;

public interface MemberSnapshotRepository {
  Optional<MemberSnapshot> findByMemberId(Long memberId);

  Optional<MemberSnapshot> findActiveByMemberId(Long memberId);

  MemberSnapshot save(MemberSnapshot snapshot);

  int upsertIfNewer(
      Long memberId,
      String name,
      String phone,
      long aggregateVersion,
      java.time.LocalDateTime updatedAt);

  int markDeletedIfNewer(
      Long memberId,
      String name,
      String phone,
      long aggregateVersion,
      java.time.LocalDateTime updatedAt);

  void deleteByMemberId(Long memberId);
}
