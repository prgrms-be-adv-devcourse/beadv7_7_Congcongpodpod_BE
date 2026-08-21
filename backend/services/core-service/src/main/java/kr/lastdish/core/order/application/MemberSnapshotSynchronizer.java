package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberSnapshotSynchronizer {
  private final MemberSnapshotRepository memberSnapshotRepository;

  public void upsert(Long memberId, String name, String phone, long aggregateVersion) {
    memberSnapshotRepository.upsertIfNewer(
        memberId, name, phone, aggregateVersion, LocalDateTime.now());
  }

  public void delete(Long memberId, String name, String phone, long aggregateVersion) {
    memberSnapshotRepository.markDeletedIfNewer(
        memberId, name, phone, aggregateVersion, LocalDateTime.now());
  }
}
