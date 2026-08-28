package kr.lastdish.core.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.infrastructure.DishJpaRepository;
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
  @Autowired private DishJpaRepository dishJpaRepository;
  @Autowired private EntityManager entityManager;

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

  @Test
  void Store나_Dish가_기간_내_변경되면_Dish가_없는_매장도_갱신_대상에_포함한다() {
    LocalDateTime from = LocalDateTime.of(2026, 8, 22, 22, 0);
    LocalDateTime to = from.plusMinutes(1);
    Store changedStore = storeJpaRepository.saveAndFlush(store("777-77-77777", from.plusHours(1)));
    Store dishChangedStore =
        storeJpaRepository.saveAndFlush(store("888-88-88888", from.plusHours(1)));
    Dish dish = dishJpaRepository.saveAndFlush(dish(dishChangedStore.getId()));

    entityManager
        .createNativeQuery("UPDATE stores SET updated_at = :updatedAt WHERE store_id = :storeId")
        .setParameter("updatedAt", from)
        .setParameter("storeId", changedStore.getId())
        .executeUpdate();
    entityManager
        .createNativeQuery("UPDATE stores SET updated_at = :updatedAt WHERE store_id = :storeId")
        .setParameter("updatedAt", from.minusMinutes(1))
        .setParameter("storeId", dishChangedStore.getId())
        .executeUpdate();
    entityManager
        .createNativeQuery("UPDATE dishes SET updated_at = :updatedAt WHERE id = :dishId")
        .setParameter("updatedAt", from.plusSeconds(30))
        .setParameter("dishId", dish.getId())
        .executeUpdate();
    entityManager.clear();

    List<Store> result = storeJpaRepository.findRenewalTargets(from, to);

    assertThat(result)
        .extracting(Store::getId)
        .containsExactly(changedStore.getId(), dishChangedStore.getId());
  }

  @Test
  void 소프트_삭제된_Dish가_기간_내_변경되면_매장을_갱신_대상으로_조회한다() {
    LocalDateTime from = LocalDateTime.of(2026, 8, 22, 22, 0);
    LocalDateTime to = from.plusMinutes(1);
    Store store = storeJpaRepository.saveAndFlush(store("999-99-99999", from.plusHours(1)));
    Dish dish = dishJpaRepository.saveAndFlush(dish(store.getId()));

    entityManager
        .createNativeQuery("UPDATE stores SET updated_at = :updatedAt WHERE store_id = :storeId")
        .setParameter("updatedAt", from.minusMinutes(1))
        .setParameter("storeId", store.getId())
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "UPDATE dishes SET is_deleted = true, updated_at = :updatedAt WHERE id = :dishId")
        .setParameter("updatedAt", from.plusSeconds(30))
        .setParameter("dishId", dish.getId())
        .executeUpdate();
    entityManager.clear();

    List<Store> result = storeJpaRepository.findRenewalTargets(from, to);

    assertThat(result).extracting(Store::getId).containsExactly(store.getId());
  }

  private Store store(String businessNumber, LocalDateTime nextClosingAt) {
    Store store =
        new Store(
            1L,
            "테스트 매장",
            businessNumber,
            "서울시 테스트 주소",
            "명정빌딩",
            "02-0000-0000",
            LocalTime.of(9, 0),
            LocalTime.of(22, 0),
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127.0),
            Category.KOREAN,
            LocalDateTime.of(2026, 8, 10, 12, 0));
    ReflectionTestUtils.setField(store, "nextClosingAt", nextClosingAt);
    return store;
  }

  private Dish dish(Long storeId) {
    return Dish.create(
        storeId,
        "테스트 상품",
        LocalDateTime.of(2026, 8, 22, 18, 0),
        "상품 설명",
        "한식",
        null,
        10L,
        BigDecimal.valueOf(10_000),
        BigDecimal.valueOf(7_000),
        LocalTime.of(18, 0),
        LocalTime.of(20, 0));
  }
}
