package kr.lastdish.core.order.application;

import kr.lastdish.core.order.domain.MemberSnapshot;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberSnapshotSynchronizer {
  private final MemberSnapshotRepository memberSnapshotRepository;

  public void upsert(Long memberId, String name, String phone) {
    MemberSnapshot snapshot =
        memberSnapshotRepository
            .findByMemberId(memberId)
            .orElseGet(() -> MemberSnapshot.create(memberId, name, phone));
    snapshot.update(name, phone);
    memberSnapshotRepository.save(snapshot);
  }

  public void delete(Long memberId) {
    memberSnapshotRepository.deleteByMemberId(memberId);
  }
}
