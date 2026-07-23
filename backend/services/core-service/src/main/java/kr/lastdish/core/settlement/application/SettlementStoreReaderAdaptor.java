package kr.lastdish.core.settlement.application;

import kr.lastdish.core.settlement.application.SettlementStoreReader;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementStoreReaderAdaptor implements SettlementStoreReader {
    private final StoreFacade storeFacade;

    @Override
    public List<Long> readSettlementTagertStoreIds() {
        return storeFacade.findSettlementTargetStoreIds();
    }
}
