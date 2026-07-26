package kr.lastdish.core.store.application;

import java.util.List;
import java.util.Optional;

import kr.lastdish.core.settlement.application.dto.StoreSettlementAccountResult;
import kr.lastdish.core.store.domain.StorePayoutAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFacade {
  private final StoreService storeService;
  private final StorePayoutAccountRepository storePayoutAccountRepository;

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }

  public List<Long> findSettlementTargetStoreIds() {
    return storeService.findSettlementTargetStoreIds();
  }

  public Optional<StoreSettlementAccountResult> findSettlementAccount(Long storeId) {
    return storePayoutAccountRepository.findByStoreId(storeId)
            .map(account ->
                    new StoreSettlementAccountResult(
                            account.getBankName(),
                            account.getAccountNumber(),
                            account.getAccountHolder()
                    )
            );
  }
}
