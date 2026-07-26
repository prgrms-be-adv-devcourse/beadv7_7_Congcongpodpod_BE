package kr.lastdish.core.settlement.application;

import java.util.List;
import java.util.Optional;

import kr.lastdish.core.settlement.application.dto.SettlementAccountData;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementStoreReaderAdaptor implements SettlementStoreReader {
  private final StoreFacade storeFacade;

  @Override
  public List<Long> readSettlementTargetStoreIds() {
    return storeFacade.findSettlementTargetStoreIds();
  }

  @Override
  public Optional<SettlementAccountData> readAccountByStoreId(Long storeId) {
    return storeFacade.findSettlementAccount(storeId)
            .map(account ->
                    new SettlementAccountData(
                            account.bankName(),
                            account.accountNumber(),
                            account.accountHolder()
                    ));
  }
}
