package kr.lastdish.core.point.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.List;
import kr.lastdish.core.point.application.PointExpirationService;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointExpirationSchedulerTest {

  @Mock private PointHistoryRepository pointHistoryRepository;
  @Mock private PointExpirationService pointExpirationService;

  @InjectMocks private PointExpirationScheduler pointExpirationScheduler;

  @Test
  void runExpirationBatch_대상이_없으면_아무것도_호출하지_않는다() {
    given(pointHistoryRepository.findMembersWithExpiringPoints()).willReturn(List.of());

    pointExpirationScheduler.runExpirationBatch();

    verify(pointExpirationService, never()).expireMemberPoints(any());
  }

  @Test
  void runExpirationBatch_대상_회원마다_소멸_처리를_호출한다() {
    given(pointHistoryRepository.findMembersWithExpiringPoints()).willReturn(List.of(1L, 2L, 3L));

    pointExpirationScheduler.runExpirationBatch();

    verify(pointExpirationService).expireMemberPoints(1L);
    verify(pointExpirationService).expireMemberPoints(2L);
    verify(pointExpirationService).expireMemberPoints(3L);
  }

  @Test
  void runExpirationBatch_한_회원이_실패해도_나머지_회원은_계속_처리된다() {
    given(pointHistoryRepository.findMembersWithExpiringPoints()).willReturn(List.of(1L, 2L, 3L));
    doThrow(new IllegalStateException("포인트 정보가 없습니다"))
        .when(pointExpirationService)
        .expireMemberPoints(2L);

    pointExpirationScheduler.runExpirationBatch();

    verify(pointExpirationService).expireMemberPoints(1L);
    verify(pointExpirationService).expireMemberPoints(2L); // 실패했지만 호출
    verify(pointExpirationService).expireMemberPoints(3L); // 2L 실패에 영향받지 않고 호출됨
  }
}
