package kr.lastdish.core.point.infrastructure;

import java.util.List;
import kr.lastdish.core.point.application.PointExpirationService;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpirationScheduler {

  private final PointHistoryRepository pointHistoryRepository;
  private final PointExpirationService pointExpirationService;

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void runExpirationBatch() {
    log.info("포인트 소멸 배치 시작");

    List<Long> targetMemberIds = pointHistoryRepository.findMembersWithExpiringPoints();
    if (targetMemberIds.isEmpty()) {
      log.info("만료 대상 포인트가 없습니다.");
      return;
    }

    int successCount = 0;
    int failCount = 0;

    for (Long memberId : targetMemberIds) {
      try {
        pointExpirationService.expireMemberPoints(memberId);
        successCount++;
      } catch (Exception e) {
        failCount++;
        log.error("회원 포인트 소멸 처리 실패. memberId={}", memberId, e);
      }
    }

    log.info("포인트 소멸 배치 종료. 성공: {}건, 실패: {}건", successCount, failCount);
  }
}
