package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import kr.lastdish.core.order.application.dto.OrderResult;
import kr.lastdish.core.order.application.dto.OrderWithStoreResult;
import kr.lastdish.core.store.application.dto.StoreQuerySnapshot;
import kr.lastdish.core.store.application.port.in.StoreQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderQueryFacadeTest {

  @Mock private OrderService orderService;
  @Mock private StoreQueryUseCase storeQueryUseCase;
  @Mock private OrderResult firstOrder;
  @Mock private OrderResult secondOrder;
  @Mock private StoreQuerySnapshot firstStore;
  @Mock private StoreQuerySnapshot secondStore;

  private OrderQueryFacade orderQueryFacade;

  @BeforeEach
  void setUp() {
    orderQueryFacade = new OrderQueryFacade(orderService, storeQueryUseCase);
  }

  @Test
  void 주문_목록의_매장명을_한번에_조회한다() {
    PageRequest pageable = PageRequest.of(0, 20);
    given(firstOrder.orderId()).willReturn(1L);
    given(firstOrder.storeId()).willReturn(10L);
    given(secondOrder.orderId()).willReturn(2L);
    given(secondOrder.storeId()).willReturn(20L);
    given(firstStore.storeId()).willReturn(10L);
    given(firstStore.storeName()).willReturn("첫 번째 매장");
    given(secondStore.storeId()).willReturn(20L);
    given(secondStore.storeName()).willReturn("두 번째 매장");
    given(orderService.getMyOrders(7L, null, pageable))
        .willReturn(new PageImpl<>(List.of(firstOrder, secondOrder), pageable, 2));
    given(storeQueryUseCase.findActiveStores(List.of(10L, 20L)))
        .willReturn(List.of(firstStore, secondStore));

    Page<OrderWithStoreResult> result = orderQueryFacade.getMyOrders(7L, null, pageable);

    assertThat(result.getContent())
        .extracting(OrderWithStoreResult::storeName)
        .containsExactly("첫 번째 매장", "두 번째 매장");
    verify(storeQueryUseCase).findActiveStores(List.of(10L, 20L));
  }

  @Test
  void 삭제된_매장은_주문_상세의_매장명을_null로_반환한다() {
    given(firstOrder.storeId()).willReturn(10L);
    given(orderService.getEachOrder(7L, 1L)).willReturn(firstOrder);
    given(storeQueryUseCase.findActiveStores(List.of(10L))).willReturn(List.of());

    OrderWithStoreResult result = orderQueryFacade.getEachOrder(7L, 1L);

    assertThat(result.order()).isSameAs(firstOrder);
    assertThat(result.storeName()).isNull();
  }
}
