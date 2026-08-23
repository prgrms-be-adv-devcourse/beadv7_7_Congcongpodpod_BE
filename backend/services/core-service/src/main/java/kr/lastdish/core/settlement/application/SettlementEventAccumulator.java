package kr.lastdish.core.settlement.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.dto.SettlementPeriod;
import kr.lastdish.core.settlement.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementEventAccumulator {
    private final SettlementRepository settlementRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final SettlementCalculator settlementCalculator;

    @Transactional
    public void accumulate(Long orderId, Long storeId, BigDecimal totalPrice, LocalDateTime orderCompletedAt) {
        if (settlementDetailRepository.existsByOrderId(orderId)) {
            log.info("이미 적립된 주문 정산 이벤트입니다. orderId={}", orderId);
            return;
        }

        YearMonth settlementMonth = YearMonth.from(orderCompletedAt);

        SettlementPeriod period = SettlementPeriod.from(settlementMonth);

        Settlement settlement = findOrCreateAccumulatingSettlement(storeId, settlementMonth, period);

        validateAccumulating(settlement);

        BigDecimal feeRate = SettlementCalculator.DEFAULT_FEE_RATE;

        long feeAmount = settlementCalculator.calculateFeeAmount(totalPrice.longValueExact(), feeRate);

        long settlementAmount = settlementCalculator.calculateSettlementAmount(totalPrice.longValueExact(), feeAmount);

        SettlementDetail detail =
                new SettlementDetail(
                        settlement.getId(),
                        orderId,
                        totalPrice.longValueExact(),
                        feeAmount,
                        feeRate,
                        settlementAmount,
                        orderCompletedAt
                );

        settlementDetailRepository.save(detail);
    }

    private Settlement findOrCreateAccumulatingSettlement(Long storeId, YearMonth settlementMonth, SettlementPeriod period) {
        Optional<Settlement> existing = settlementRepository.findByStoreIdAndSettlementMonth(storeId, settlementMonth);

        if (existing.isPresent()) {
            return existing.get();
        }

        settlementRepository.insertAccumulatingIfAbsent(
                storeId,
                settlementMonth,
                period.periodStart(),
                period.periodEnd(),
                SettlementCalculator.DEFAULT_FEE_RATE
        );

        return settlementRepository.findByStoreIdAndSettlementMonth(storeId, settlementMonth)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "이벤트 정산 정보를 생성하지 못했습니다."));
    }

    private void validateAccumulating(Settlement settlement) {
        if (settlement.getSettlementStatus() != SettlementStatus.ACCUMULATING) {
            throw new BusinessException(CommonErrorCode.INVALID_STATE, "적립 중인 정산에만 주문 정산 내역을 추가할 수 있습니다.");
        }
    }
}
