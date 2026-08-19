package kr.lastdish.core.point.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.point.domain.Point;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import kr.lastdish.core.point.domain.PointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointExpirationServiceTest {

    @Mock private PointRepository pointRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;

    @InjectMocks private PointExpirationService pointExpirationService;

    @Test
    void expireMemberPoints_만료된_적립건이_있으면_잔액이_차감되고_EXPIRE_이력이_기록된다() {
        Point point = Point.createDefault(1L);
        point.earn(new BigDecimal("500"));
        given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

        PointHistory expired = PointHistory.recordEarn(1L, 100L, new BigDecimal("500"), new BigDecimal("500"));
        given(pointHistoryRepository.findExpiringHistoriesByMember(1L)).willReturn(List.of(expired));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        pointExpirationService.expireMemberPoints(1L);

        assertThat(point.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(expired.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void expireMemberPoints_만료건이_없으면_아무것도_하지_않는다() {
        Point point = Point.createDefault(1L);
        given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));
        given(pointHistoryRepository.findExpiringHistoriesByMember(1L)).willReturn(List.of());

        pointExpirationService.expireMemberPoints(1L);

        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void expireMemberPoints_Point가_없으면_예외가_발생한다() {
        given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointExpirationService.expireMemberPoints(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expireMemberPoints_여러_건이_만료되면_합산되어_한_번에_차감된다() {
        Point point = Point.createDefault(1L);
        point.earn(new BigDecimal("1000"));
        given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

        PointHistory h1 = PointHistory.recordEarn(1L, 100L, new BigDecimal("300"), new BigDecimal("300"));
        PointHistory h2 = PointHistory.recordEarn(1L, 101L, new BigDecimal("200"), new BigDecimal("500"));
        given(pointHistoryRepository.findExpiringHistoriesByMember(1L)).willReturn(List.of(h1, h2));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        pointExpirationService.expireMemberPoints(1L);

        assertThat(point.getBalance()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(h1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(h2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}