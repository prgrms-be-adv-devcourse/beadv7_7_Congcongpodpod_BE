package kr.lastdish.core.order.domain;

import java.util.Optional;

public interface MemberSnapshotRepository {
  Optional<MemberSnapshot> findByMemberId(Long memberId);

  MemberSnapshot save(MemberSnapshot snapshot);

  void deleteByMemberId(Long memberId);
}
