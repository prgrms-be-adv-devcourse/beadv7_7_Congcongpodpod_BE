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
                command.storeDetailAddress(),
            command.storePhone(),
            command.openTime(),
            command.closeTime(),
            command.latitude(),
            command.longitude(),
            command.category(),
            LocalDateTime.now(BUSINESS_ZONE));

    command.holidays().forEach(store::addHoliday);

    Store savedStore = storeRepository.save(store);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendCreatedEvent(savedStore);

    return StoreResult.from(savedStore);
  }

  @Transactional
  public StoreResult update(Long storeId, Long memberId, UpdateStoreCommand command) {
    Store store = getOwnedStoreWithLock(storeId, memberId);

    store.update(
        command.storeName(),
        command.storeAddress(),
        command.storeDetailAddress(),
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
    Store store = getOwnedStoreWithLock(storeId, memberId);

    store.changeStatus(status);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendStatusChangedEvent(store);

    return StoreResult.from(store);
  }

  // 삭제 시 매장 조회, 수정, 상태 변경 제외, 매장 재등록은 재가입 필요
  // 매장 soft delete 시 휴무일 hard delete
  @Transactional
  public void deleteStore(Long storeId, Long memberId) {
    Store store = getOwnedStoreWithLock(storeId, memberId);

    store.delete();
    payoutAccountRepository.deleteByStoreId(storeId);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendDeletedEvent(store);
  }

  // 이벤트를 발행하는 변경 메서드용 — 행 잠금으로 eventVersion 경합을 막고 소유권을 검증한다.
  private Store getOwnedStoreWithLock(Long storeId, Long memberId) {
    Store store =
        storeRepository
            .findWithLockById(storeId)
            .orElseThrow(
                () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));

    if (!store.isOwnedBy(memberId)) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "해당 매장을 수정할 권한이 없습니다.");
    }

    return store;
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

  /** 주문 직전 매장이 주문을 받을 수 있는 영업 상태인지 확인한다. 영업 상태 플래그와 영업시간을 함께 본다. */
  public void validateOpen(Long storeId, LocalDateTime now) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_STORE_CLOSED));

    if (!store.isOpenAt(now)) {
      throw new BusinessException(ErrorCode.ORDER_STORE_CLOSED);
    }
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
    findStore(storeId).validatePickupTime(pickupStartTime, pickupEndTime);
  }

  /**
   * 매장 영업일 기준으로 픽업 마감 일시를 확정한다.
   *
   * <p>픽업 창이 영업시간 안에 있는지는 다시 보지 않는다 — 그 불변식은 Dish 등록·수정 시점에 이미 검증되며, 주문 시점에 다시 던지면 구매자가 상품 등록용
   * 에러(DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS)를 받게 된다.
   */
  public LocalDateTime calculatePickupDeadline(
      Long storeId, LocalTime pickupEndTime, LocalDateTime now) {
    return findStore(storeId).calculatePickupDeadline(now, pickupEndTime);
  }

  private Store findStore(Long storeId) {
    return storeRepository
        .findById(storeId)
        .orElseThrow(
            () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "매장을 찾을 수 없습니다."));
  }

  // 검색 문서 생성을 요청하는 매장 생성 이벤트를 Outbox에 기록한다.
  private void appendCreatedEvent(Store store) {
    long aggregateVersion = store.nextEventVersion();
    StoreCreatedPayload payload = new StoreCreatedPayload(store.getId());

    StoreCreatedEvent event =
        new StoreCreatedEvent(
            UUID.randomUUID(),
            StoreCreatedEvent.SCHEMA_VERSION,
            store.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }

  // 검색 문서 갱신을 요청하는 매장 정보 변경 이벤트를 Outbox에 기록한다.
  private void appendChangedEvent(Store store) {
    StoreChangedPayload payload = new StoreChangedPayload(store.getId());

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

  // 검색 문서 갱신을 요청하는 매장 상태 변경 이벤트를 Outbox에 기록한다.
  private void appendStatusChangedEvent(Store store) {
    StoreStatusChangedPayload payload = new StoreStatusChangedPayload(store.getId());

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

  // 검색 문서 삭제를 요청하는 매장 삭제 이벤트를 Outbox에 기록한다.
  private void appendDeletedEvent(Store store) {
    StoreDeletedPayload payload = new StoreDeletedPayload(store.getId());

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
