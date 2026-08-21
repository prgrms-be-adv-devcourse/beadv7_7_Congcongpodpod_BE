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
  public MemberSnapshot save(MemberSnapshot snapshot) {
    return memberSnapshotJpaRepository.save(snapshot);
  }

  @Override
  public void deleteByMemberId(Long memberId) {
    memberSnapshotJpaRepository.deleteById(memberId);
  }
}
