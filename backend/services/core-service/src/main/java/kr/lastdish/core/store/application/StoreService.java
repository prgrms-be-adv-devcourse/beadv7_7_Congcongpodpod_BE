package kr.lastdish.core.store.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.store.application.dto.*;
import kr.lastdish.core.store.domain.*;
import kr.lastdish.core.store.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final StoreRepository storeRepository;
  private final StorePayoutAccountRepository payoutAccountRepository;
  private final OutboxEventWriter outboxEventWriter;

  @Transactional
  public StoreResult register(RegisterStoreCommand command) {
    if (storeRepository.existsByMemberId(command.memberId())) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "회원은 하나의 매장만 등록할 수 있습니다.");
    }

    if (storeRepository.existsByBusinessNumber(command.businessNumber())) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "이미 등록된 사업자등록번호입니다.");
    }

    Store store =
        new Store(
            command.memberId(),
            command.storeName(),
            command.businessNumber(),
            command.storeAddress(),
            command.storePhone(),
            command.openTime(),
            command.closeTime(),
            command.latitude(),
            command.longitude(),
            command.category());

    command.holidays().forEach(store::addHoliday);
    store.rescheduleNextClosingAt(LocalDateTime.now(BUSINESS_ZONE));

    Store savedStore = storeRepository.save(store);

    return StoreResult.from(savedStore);
  }

  @Transactional
  public StoreResult update(Long storeId, Long memberId, UpdateStoreCommand command) {
    Store store =
        storeRepository
            .findWithLockById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    store.update(
        command.storeName(),
        command.storeAddress(),
        command.storePhone(),
        command.openTime(),
        command.closeTime(),
        command.latitude(),
        command.longitude(),
        command.category());

    store.replaceHolidays(command.holidays());
    store.rescheduleNextClosingAt(LocalDateTime.now(BUSINESS_ZONE));

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendChangedEvent(store);

    return StoreResult.from(store);
  }

  @Transactional
  public StoreResult changeStatus(Long storeId, Long memberId, StoreStatus status) {
    Store store =
        storeRepository
            .findWithLockById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    store.changeStatus(status);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendStatusChangedEvent(store);

    return StoreResult.from(store);
  }

  // 삭제 시 매장 조회, 수정, 상태 변경 제외, 매장 재등록은 재가입 필요
  // 매장 soft delete 시 휴무일 hard delete
  @Transactional
  public void deleteStore(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findWithLockById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    store.delete();
    payoutAccountRepository.deleteByStoreId(storeId);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendDeletedEvent(store);
  }

  public Store getOwnedStore(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    return store;
  }

  // Seller 본인 매장 조회 — 지금은 회원당 매장 1개 제약이라 0~1건이지만, 나중에 여러 개로 늘어나도
  // API 모양이 바뀌지 않도록 목록으로 반환한다.
  public List<StoreResult> getMyStores(Long memberId) {
    return storeRepository
        .findByMemberId(memberId)
        .map(StoreResult::from)
        .map(List::of)
        .orElseGet(List::of);
  }

  // 매장 상세 조회
  public StoreResult getStore(Long storeId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    return StoreResult.from(store);
  }

  // 매장 정산 계좌
  @Transactional
  public PayoutAccountResult registerPayoutAccount(
      Long storeId, Long memberId, String bankName, String accountNumber, String accountHolder) {
    getOwnedStore(storeId, memberId);

    if (payoutAccountRepository.existsByStoreId(storeId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "이미 등록된 정산 계좌가 있습니다.");
    }

    StorePayoutAccount payoutAccount =
        new StorePayoutAccount(storeId, bankName, accountNumber, accountHolder);

    StorePayoutAccount savedAccount = payoutAccountRepository.save(payoutAccount);

    return PayoutAccountResult.from(savedAccount);
  }

  @Transactional
  public PayoutAccountResult updatePayoutAccount(
      Long storeId, Long memberId, String bankName, String accountNumber, String accountHolder) {
    getOwnedStore(storeId, memberId);

    StorePayoutAccount payoutAccount =
        payoutAccountRepository
            .findByStoreId(storeId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        CommonErrorCode.ENTITY_NOT_FOUND, "등록된 정산 계좌를 찾을 수 없습니다."));

    payoutAccount.update(bankName, accountNumber, accountHolder);

    return PayoutAccountResult.from(payoutAccount);
  }

  // StoreFacade 검증 메서드
  public void validateSeller(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(ErrorCode.ORDER_NOT_SELLER);
    }
  }

  @Transactional(readOnly = true)
  public List<Long> findSettlementTargetStoreIds() {
    return storeRepository.findAllActiveStoreIds();
  }

  public void validateDishPickupTime(
      Long storeId, LocalTime pickupStartTime, LocalTime pickupEndTime) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    int businessDuration = forwardMinutes(store.getOpenTime(), store.getCloseTime());
    int pickupStartOffset = forwardMinutes(store.getOpenTime(), pickupStartTime);
    int pickupEndOffset = forwardMinutes(store.getOpenTime(), pickupEndTime);

    boolean isWithinBusinessHours =
        pickupEndOffset <= businessDuration && pickupStartOffset <= pickupEndOffset;

    if (!isWithinBusinessHours) {
      throw new BusinessException(ErrorCode.DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS);
    }
  }

  private int forwardMinutes(LocalTime from, LocalTime to) {
    int minutes = to.toSecondOfDay() / 60 - from.toSecondOfDay() / 60;
    return Math.floorMod(minutes, 24 * 60);
  }

  private void appendChangedEvent(Store store) {
    StoreChangedPayload payload =
        new StoreChangedPayload(
            store.getId(),
            store.getStoreName(),
            store.getStoreAddress(),
            store.getStorePhone(),
            store.getOpenTime(),
            store.getCloseTime(),
            store.getLatitude(),
            store.getLongitude(),
            store.getCategory());

    long aggregateVersion = store.nextEventVersion();

    StoreChangedEvent event =
        new StoreChangedEvent(
            UUID.randomUUID(),
            StoreChangedEvent.SCHEMA_VERSION,
            store.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }

  private void appendStatusChangedEvent(Store store) {
    StoreStatusChangedPayload payload =
        new StoreStatusChangedPayload(store.getId(), store.getStatus());

    long aggregateVersion = store.nextEventVersion();

    StoreStatusChangedEvent event =
        new StoreStatusChangedEvent(
            UUID.randomUUID(),
            StoreStatusChangedEvent.SCHEMA_VERSION,
            store.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }

  private void appendDeletedEvent(Store store) {
    StoreDeletedPayload payload = new StoreDeletedPayload(store.getId(), store.isDeleted());

    long aggregateVersion = store.nextEventVersion();

    StoreDeletedEvent event =
        new StoreDeletedEvent(
            UUID.randomUUID(),
            StoreDeletedEvent.SCHEMA_VERSION,
            store.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }
}
