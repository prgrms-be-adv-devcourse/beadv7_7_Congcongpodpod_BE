package kr.lastdish.core.store.application;

import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.application.dto.StoreResult;
import kr.lastdish.core.store.application.dto.UpdateStoreCommand;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.domain.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResult register(
            RegisterStoreCommand command
    ) {
        if (
                storeRepository.existsByMemberId(
                        command.memberId()
                )
        ) {
            throw new IllegalStateException(
                    "회원은 하나의 매장만 등록할 수 있습니다."
            );
        }

        if (
                storeRepository.existsByBusinessNumber(
                        command.businessNumber()
                )
        ) {
            throw new IllegalStateException(
                    "이미 등록된 사업자등록번호입니다."
            );
        }

        Store store = new Store(
                command.memberId(),
                command.storeName(),
                command.businessNumber(),
                command.storeAddress(),
                command.storePhone(),
                command.openTime(),
                command.closeTime(),
                command.latitude(),
                command.longitude()
        );

        command.holidays().forEach(store::addHoliday);

        Store savedStore = storeRepository.save(store);

        return StoreResult.from(savedStore);
    }

    @Transactional
    public StoreResult update(Long storeId, Long memberId, UpdateStoreCommand command) {
        Store store = getOwnedStore(storeId, memberId);

        store.update(
                command.storeName(),
                command.storeAddress(),
                command.storePhone(),
                command.openTime(),
                command.closeTime(),
                command.latitude(),
                command.longitude()
        );

        store.replaceHolidays(command.holidays());

        return StoreResult.from(store);
    }

    private Store getOwnedStore(Long storeId, Long memberId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다."));

        if (!store.isOwnedBy(memberId)) {
            throw new IllegalStateException("해당 매장을 수정할 권한이 없습니다.");
        }

        return store;
    }
}