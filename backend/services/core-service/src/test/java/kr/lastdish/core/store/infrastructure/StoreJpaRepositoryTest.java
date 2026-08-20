package kr.lastdish.core.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StoreJpaRepositoryTest {

  @Autowired private StoreJpaRepository storeJpaRepository;

  @Test
  void 마감_정각과_마감_시간이_지난_매장만_마감_대상으로_조회한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 22, 0);
    Store beforeClosing = store("111-11-11111", now.plusMinutes(1));
    Store atClosing = store("222-22-22222", now);
    Store afterClosing = store("333-33-33333", now.minusMinutes(1));
    storeJpaRepository.saveAllAndFlush(List.of(beforeClosing, atClosing, afterClosing));

    List<Long> storeIds = storeJpaRepository.findStoreIdsReadyToClose(now);

    assertThat(storeIds).containsExactly(atClosing.getId(), afterClosing.getId());
  }

  @Test
  void 매장을_저장하면_updatedAt이_채워진다() {
    Store store = store("444-44-44444", LocalDateTime.of(2026, 8, 10, 22, 0));

    Store saved = storeJpaRepository.saveAndFlush(store);

    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  @Test
  void 소프트_삭제된_매장은_조회되지_않는다() {
    Store store =
        storeJpaRepository.saveAndFlush(
            store("666-66-66666", LocalDateTime.of(2026, 8, 10, 22, 0)));

    store.delete();
    storeJpaRepository.flush();

    assertThat(storeJpaRepository.findByIdAndDeletedFalse(store.getId())).isEmpty();
  }

  private Store store(String businessNumber, LocalDateTime nextClosingAt) {
    Store store =
        new Store(
            1L,
            "테스트 매장",
            businessNumber,
            "서울시 테스트 주소",
            "02-0000-0000",
            LocalTime.of(9, 0),
            LocalTime.of(22, 0),
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127.0),
            Category.KOREAN);
    ReflectionTestUtils.setField(store, "nextClosingAt", nextClosingAt);
    return store;
  }
}
