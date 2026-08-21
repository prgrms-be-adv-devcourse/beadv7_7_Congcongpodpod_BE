package kr.lastdish.core.order.infrastructure;

import java.util.Optional;
import kr.lastdish.core.order.domain.MemberSnapshot;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberSnapshotRepositoryImpl implements MemberSnapshotRepository {
  private final MemberSnapshotJpaRepository memberSnapshotJpaRepository;

  @Override
  public Optional<MemberSnapshot> findByMemberId(Long memberId) {
    return memberSnapshotJpaRepository.findById(memberId);
  }

  @Override
  public Optional<MemberSnapshot> findActiveByMemberId(Long memberId) {
    return memberSnapshotJpaRepository.findByMemberIdAndDeletedFalse(memberId);
  }

  @Override
  public MemberSnapshot save(MemberSnapshot snapshot) {
    return memberSnapshotJpaRepository.save(snapshot);
  }

  @Override
  public int upsertIfNewer(
      Long memberId,
      String name,
      String phone,
      long aggregateVersion,
      java.time.LocalDateTime updatedAt) {
    return memberSnapshotJpaRepository.upsertIfNewer(
        memberId, name, phone, aggregateVersion, updatedAt);
  }

  @Override
  public int markDeletedIfNewer(
      Long memberId,
      String name,
      String phone,
      long aggregateVersion,
      java.time.LocalDateTime updatedAt) {
    return memberSnapshotJpaRepository.markDeletedIfNewer(
        memberId, name, phone, aggregateVersion, updatedAt);
  }

  @Override
  public void deleteByMemberId(Long memberId) {
    memberSnapshotJpaRepository.deleteById(memberId);
  }
}
