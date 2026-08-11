package kr.lastdish.core.store.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.dto.StoreSettlementAccountResult;
import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.port.out.SellerRoleGrantPort;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import kr.lastdish.core.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFacade {
  private final StoreService storeService;
  private final StorePayoutAccountRepository storePayoutAccountRepository;
  private final SellerRoleGrantPort sellerRoleGrantPort;
  private final StoreRepository storeRepository;

  @Transactional
  public StoreResult register(RegisterStoreCommand command) {
    StoreResult result = storeService.register(command);
    sellerRoleGrantPort.grantSellerRole(command.memberId());
    return result;
  }

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }

  public List<Long> findSettlementTargetStoreIds() {
    return storeService.findSettlementTargetStoreIds();
  }

  @Transactional
  public List<Long> claimStoresReadyToClose(LocalDateTime now) {
    List<Store> stores = storeRepository.findStoresReadyToClose(now);
    stores.forEach(store -> store.rescheduleNextClosingAt(now));
    return stores.stream().map(Store::getId).toList();
  }

  public Optional<StoreSettlementAccountResult> findSettlementAccount(Long storeId) {
    return storePayoutAccountRepository
        .findByStoreId(storeId)
        .map(
            account ->
                new StoreSettlementAccountResult(
                    account.getBankName(), account.getAccountNumber(), account.getAccountHolder()));
  }

  public Long findStoreIdByMemberId(Long memberId) {
    return storeRepository
        .findByMemberIdForSettlement(memberId)
        .map(Store::getId)
        .orElseThrow(
            () ->
                new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "회원이 소유한 매장을 찾을 수 없습니다."));
  }
}
