package kr.lastdish.core.store.application;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.settlement.application.dto.StoreSettlementAccountResult;
import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.port.out.SellerRoleGrantPort;
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
  private final SellerRoleGrantPort sellerRoleGrantPort;

  public StoreResult register(RegisterStoreCommand command) {
    StoreResult result = storeService.register(command);

    // TODO: 역할 부여 호출 실패 시 재시도와 멱등성을 보장해 매장과 회원 권한의 불일치를 복구한다.
    sellerRoleGrantPort.grantSellerRole(command.memberId());

    return result;
  }

  public void validateStoreOwner(Long storeId, Long memberId) {
    storeService.validateSeller(storeId, memberId);
  }

  public List<Long> findSettlementTargetStoreIds() {
    return storeService.findSettlementTargetStoreIds();
  }

  public Optional<StoreSettlementAccountResult> findSettlementAccount(Long storeId) {
    return storePayoutAccountRepository
        .findByStoreId(storeId)
        .map(
            account ->
                new StoreSettlementAccountResult(
                    account.getBankName(), account.getAccountNumber(), account.getAccountHolder()));
  }
}
