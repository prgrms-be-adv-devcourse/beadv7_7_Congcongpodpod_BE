package kr.lastdish.core.settlement.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import kr.lastdish.core.settlement.application.dto.SettlementCreateResult;
import kr.lastdish.core.settlement.application.dto.SettlementOrderData;
import kr.lastdish.core.settlement.application.dto.SettlementPeriod;
import kr.lastdish.core.settlement.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
  private final SettlementCalculator settlementCalculator;
  private final SettlementOrderReader settlementOrderReader;
  private final SettlementRepository settlementRepository;
  private final SettlementDetailRepository settlementDetailRepository;

  /*
   * 한 매장의 월 정산을 하나의 독립된 트랜잭션으로 생성합니다.
   *
   * Settlement 또는 SettlementDetail 저장 중 예외가 발생하면
   * 해당 매장의 정산 데이터 전체가 롤백됩니다.
   * Failed의 경우 정산 데이터가 쌓이지 않음
   *
   * -> 기본 구현 후 재처리 방식 변경, 진행 상황부터 이어서 정산되도록
   */

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SettlementCreateResult createMonthlySettlement(Long storeId, YearMonth settlementMonth) {
    // 이미 정산된 매장이면 skip
    if (settlementRepository.existsByStoreIdAndSettlementMonth(storeId, settlementMonth)) {
      return SettlementCreateResult.skipped(storeId);
    }

    SettlementPeriod period = SettlementPeriod.from(settlementMonth);

    // Order로부터 매장 주문 내역 가져오기
    List<SettlementOrderData> orders =
        settlementOrderReader.readSettlementOrders(
            storeId, period.periodStart(), period.periodEnd());

    // 이미 처리된 주문 제거
    List<SettlementOrderData> unsettledOrders = excludeSettledOrders(orders);

    // 처리되지 않은 주문이 없다면 매장 skip
    if (unsettledOrders.isEmpty()) {
      return SettlementCreateResult.skipped(storeId);
    }

    List<OrderSettlementAmount> calculatedOrders = calculateOrders(unsettledOrders);

    // 정산 생성
    Settlement settlement = createSettlement(storeId, settlementMonth, period, calculatedOrders);

    Settlement savedSettlement = settlementRepository.save(settlement);

    List<SettlementDetail> settlementDetails =
        createSettlementDetails(savedSettlement, calculatedOrders);

    settlementDetailRepository.saveAll(settlementDetails);

    // 정산 완료 처리
    savedSettlement.complete();

    return SettlementCreateResult.created(
        storeId, savedSettlement.getId(), settlementDetails.size());
  }

  private List<SettlementOrderData> excludeSettledOrders(List<SettlementOrderData> orders) {
    if (orders == null || orders.isEmpty()) {
      return List.of();
    }

    Set<Long> settledOrderIds =
        settlementDetailRepository.findSettledOrderIds(
            orders.stream().map(SettlementOrderData::orderId).toList());

    return orders.stream().filter(order -> !settledOrderIds.contains(order.orderId())).toList();
  }

  // 각 주문을 수수료 계산(정산)하여 리스트로 반환
  private List<OrderSettlementAmount> calculateOrders(List<SettlementOrderData> orders) {
    BigDecimal feeRate = SettlementCalculator.DEFAULT_FEE_RATE;

    return orders.stream().map(order -> calculateOrder(order, feeRate)).toList();
  }

  // 각 주문 수수료 계산
  private OrderSettlementAmount calculateOrder(SettlementOrderData order, BigDecimal feeRate) {
    long feeAmount = settlementCalculator.calculateFeeAmount(order.salesAmount(), feeRate);

    long settlementAmount =
        settlementCalculator.calculateSettlementAmount(order.salesAmount(), feeAmount);

    return new OrderSettlementAmount(
        order, order.salesAmount(), feeRate, feeAmount, settlementAmount);
  }

  // 정산 생성
  private Settlement createSettlement(
      Long storeId,
      YearMonth settlementMonth,
      SettlementPeriod period,
      List<OrderSettlementAmount> calculatedOrders) {
    // 정산 전체 판매액 합산
    long grossAmount =
        calculatedOrders.stream().mapToLong(OrderSettlementAmount::salesAmount).sum();

    // 정산 전체 수수료 합산
    long feeAmount = calculatedOrders.stream().mapToLong(OrderSettlementAmount::feeAmount).sum();

    // 정산 전체 정산액 합산
    long settlementAmount =
        calculatedOrders.stream().mapToLong(OrderSettlementAmount::settlementAmount).sum();

    return new Settlement(
        storeId,
        settlementMonth,
        period.periodStart(),
        period.periodEnd(),
        calculatedOrders.size(),
        grossAmount,
        SettlementCalculator.DEFAULT_FEE_RATE,
        feeAmount,
        settlementAmount);
  }

  // 정산된 주문 리스트 -> 정산 내역으로 변환
  private List<SettlementDetail> createSettlementDetails(
      Settlement settlement, List<OrderSettlementAmount> calculatedOrders) {
    return calculatedOrders.stream()
        .map(
            calculated ->
                new SettlementDetail(
                    settlement.getId(),
                    calculated.order().orderId(),
                    calculated.salesAmount(),
                    calculated.feeAmount(),
                    calculated.feeRate(),
                    calculated.settlementAmount(),
                    calculated.order().orderCompletedAt()))
        .toList();
  }

  // 정산된 주문 데이터
  private record OrderSettlementAmount(
      SettlementOrderData order,
      long salesAmount,
      BigDecimal feeRate,
      long feeAmount,
      long settlementAmount) {}
}
