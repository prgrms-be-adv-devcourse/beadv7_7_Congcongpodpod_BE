package kr.lastdish.core.settlement.application;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.batch.SettlementTransactionalManager;
import kr.lastdish.core.settlement.application.dto.*;
import kr.lastdish.core.settlement.domain.*;
import kr.lastdish.core.settlement.presentation.dto.SettlementDetailResponse;
import kr.lastdish.core.settlement.presentation.dto.SettlementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
  private final SettlementCalculator settlementCalculator;
  private final SettlementOrderReader settlementOrderReader;
  private final SettlementRepository settlementRepository;
  private final SettlementDetailRepository settlementDetailRepository;
  private final SettlementStoreReader settlementStoreReader;
  private final SettlementTransactionalManager settlementTransactionalManager;

  /*
   * 재처리 방식 개선, 실패 결과는 저장되고 정산 재시도 시 재반영
   * 정산 시작 시 매장의 정산 상태를 Processing으로 저장
   * Detail 저장 중 예외 발생 시 매장의 정산 상태는 Failed로 변경, Detail만 롤백
   * 각 매장의 정산 결과를 log로 출력
   *
   * Transactional 범위 변경
   * 매장별 트랜잭션 -> 매장의 각각의 DB 변경은 별도의 트랜잭션으로 분리
   * -> 정산의 상태 관리를 위함, 현재 구조 상 실패할 일이 적으나,
   *    추후 서비스가 커졌을 때 타 모듈, 서비스와의 통신 오류나 실패 원인별 세부 처리를 위해
   *    정산의 상태를 DB에 적재하기 위함
   *
   * DB 커넥션 및 계산 결과 오류 시 failed 처리 -> 재시도 시 성공 가능
   * 정산 계좌 미등록의 경우 정산하지 않고 로그만 출력하여 관리 -> 계좌 미등록 상태가 유지된다면 재시도 시 실패
   */

  public SettlementProcessResult processMonthlySettlement(Long storeId, YearMonth settlementMonth) {
    // 이미 존재하는 정산이 있는지 조회
    Settlement existing =
        settlementRepository.findByStoreIdAndSettlementMonth(storeId, settlementMonth).orElse(null);

    // 정산 시작 전 초기화
    boolean retry = false;
    Settlement settlement;
    List<SettlementOrderData> unsettledOrders = null;

    if (existing != null) {
      // 정산이 이미 존재함 (Processing, Failed), Completed는 Tasklet에서 제거 후 정산 실행

      if (existing.getSettlementStatus() == SettlementStatus.PROCESSING) {
        return SettlementProcessResult.skipped(
            storeId, existing.getId(), "현재 처리 중이거나 복구 확인이 필요한 정산입니다.");
        // 정산이 시작되지 않았거나 Failed 상태인 정산만 시도하기 위해 Processing을 skip하는 로직을 추가했지만,
        // Processing 상태로 재시도될 경우가 있을까,,
        // -> Processing에서 상태 변경이 되지 않고 오류 발생 시밖에 없음, 이 부분은 수동 처리
      }

      // Failed 정산의 경우 재시도, 기존 정산을 상태 변경 후 settlement 변수에 할당
      settlement = settlementTransactionalManager.restart(existing.getId());
      retry = true;
    } else {
      // 해당 정산 없음, 정산 생성
      SettlementPeriod period = SettlementPeriod.from(settlementMonth);

      // Order로부터 매장 주문 내역 가져오기
      List<SettlementOrderData> orders =
          settlementOrderReader.readSettlementOrders(
              storeId, period.periodStart(), period.periodEnd());

      // 이미 처리된 주문 제거
      unsettledOrders = excludeSettledOrders(orders);

      if (unsettledOrders == null || unsettledOrders.isEmpty()) {
        return SettlementProcessResult.skipped(storeId, null, "정산 대상 주문이 없습니다.");
      }

      Optional<SettlementAccountData> account = settlementStoreReader.readAccountByStoreId(storeId);

      // 정산 계좌 미등록은 정산 처리 실패가 아닌 정산 조건 미충족으로 skip
      if (account.isEmpty()) {
        return SettlementProcessResult.skipped(storeId, null, "정산 계좌가 등록되지 않았습니다.");
      }

      // 정산 생성, 기본값으로 생성, complete 시 삽입
      settlement =
          settlementTransactionalManager.start(
              setSettlement(storeId, settlementMonth, period, account.get()));
    }

    try {
      // 재시도인 경우만 매장 주문 내역 가져옮, 아닌 경우 상단 else문에서 가져온 주문 내역 사용
      if (retry) {
        SettlementPeriod period = SettlementPeriod.from(settlementMonth);

        // Order로부터 매장 주문 내역 가져오기
        List<SettlementOrderData> orders =
            settlementOrderReader.readSettlementOrders(
                storeId, period.periodStart(), period.periodEnd());

        // 이미 처리된 주문 제거
        unsettledOrders = excludeSettledOrders(orders);
      }
      // 계산 오류, 제약조건 오류, DB 커넥션 오류 시에만 fail로 저장
      List<OrderSettlementAmount> calculatedOrders =
          unsettledOrders.stream()
              .map(order -> calculateOrder(order, SettlementCalculator.DEFAULT_FEE_RATE))
              .toList();

      settlementTransactionalManager.complete(settlement.getId(), calculatedOrders);

      return retry
          ? SettlementProcessResult.retried(storeId, settlement.getId())
          : SettlementProcessResult.created(storeId, settlement.getId());
    } catch (Exception e) {
      String failureReason = extractFailureReason(e);

      settlementTransactionalManager.fail(settlement.getId(), failureReason);

      return SettlementProcessResult.failed(storeId, settlement.getId(), failureReason);
    }
  }

  @Transactional(readOnly = true)
  public Page<SettlementResponse> getSettlements(Long memberId, String role, Pageable pageable) {
    if (!role.equals("SELLER")) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "판매자만 정산 내역을 조회할 수 있습니다.");
    }
    Long storeId = settlementStoreReader.readStoreIdByMemberId(memberId);

    return settlementRepository.findAllByStoreId(storeId, pageable).map(SettlementResponse::from);
  }

  @Transactional(readOnly = true)
  public SettlementDetailResponse getSettlement(Long memberId, String role, Long settlementId) {
    if (!role.equals("SELLER")) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "판매자만 정산 내역을 조회할 수 있습니다.");
    }
    Long storeId = settlementStoreReader.readStoreIdByMemberId(memberId);

    Settlement settlement =
        settlementRepository
            .findByIdAndStoreId(settlementId, storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "정산 정보를 찾을 수 없습니다."));

    List<SettlementDetail> details = settlementDetailRepository.findAllBySettlementId(settlementId);

    return SettlementDetailResponse.of(settlement, details);
  }

  // 정산 테스트용 Truncate_테스트 후 제거 필수
  @Transactional
  public void initializeSettlement() {
    settlementRepository.truncateAllSettlementData();
    log.warn("-------------정산 성능 테스트용 데이터 전체 초기화 완료 settlements settlement_details---------------");
  }

  public List<Long> excludeSettledStore(List<Long> storeIds) {
    if (storeIds == null || storeIds.isEmpty()) {
      return List.of();
    }

    Set<Long> settledStoreIds = settlementRepository.findSettledStoreIds(storeIds);

    return storeIds.stream().filter(storeId -> !settledStoreIds.contains(storeId)).toList();
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

  // 정산 start 세팅
  private Settlement setSettlement(
      Long storeId,
      YearMonth settlementMonth,
      SettlementPeriod period,
      SettlementAccountData account) {

    return new Settlement(
        storeId,
        settlementMonth,
        period.periodStart(),
        period.periodEnd(),
        0,
        0,
        SettlementCalculator.DEFAULT_FEE_RATE,
        0,
        0,
        account.bankName(),
        account.accountNumber(),
        account.accountHolder());
  }

  // 각 주문 수수료 계산
  private OrderSettlementAmount calculateOrder(SettlementOrderData order, BigDecimal feeRate) {
    long feeAmount = settlementCalculator.calculateFeeAmount(order.salesAmount(), feeRate);

    long settlementAmount =
        settlementCalculator.calculateSettlementAmount(order.salesAmount(), feeAmount);

    return new OrderSettlementAmount(
        order, order.salesAmount(), feeRate, feeAmount, settlementAmount);
  }

  private String extractFailureReason(Exception exception) {
    Throwable cause = exception;

    while (cause.getCause() != null) {
      cause = cause.getCause();
    }

    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }
}
