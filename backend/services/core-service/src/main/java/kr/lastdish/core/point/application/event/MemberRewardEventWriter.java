package kr.lastdish.core.point.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.level.domain.DishLevel;
import kr.lastdish.core.point.domain.event.MemberRewardEvent;
import kr.lastdish.core.point.domain.event.MemberRewardPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberRewardEventWriter {

  private static final long NOT_APPLICABLE_VERSION = 0L;

  private final OutboxEventWriter outboxEventWriter;

  public void append(
      Long memberId, BigDecimal earnedAmount, boolean upgraded, DishLevel currentLevel) {
    String title = upgraded ? "새로운 레벨 달성!" : "포인트 적립 완료!";
    String body =
        upgraded
            ? "축하합니다! " + currentLevel.getDisplayName() + " 레벨이 되셨습니다."
            : earnedAmount + "P가 적립되었습니다. 현재 레벨을 확인해보세요.";

    MemberRewardEvent event =
        new MemberRewardEvent(
            UUID.randomUUID(),
            MemberRewardEvent.SCHEMA_VERSION,
            memberId,
            NOT_APPLICABLE_VERSION,
            new MemberRewardPayload(
                memberId, "POINT_EARNED", title, body, null, "DISH_REPORT", null),
            Instant.now());

    outboxEventWriter.append(event);
  }


  public void appendDishReportCompleted(Long memberId) {
    MemberRewardEvent event =
            new MemberRewardEvent(
                    UUID.randomUUID(),
                    MemberRewardEvent.SCHEMA_VERSION,
                    memberId,
                    NOT_APPLICABLE_VERSION,
                    new MemberRewardPayload(
                            memberId, "DISH_REPORT_COMPLETED", null, null, null, "DISH_REPORT", null),
                    Instant.now());

    outboxEventWriter.append(event);
  }
}
