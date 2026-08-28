package kr.lastdish.core.order.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.lastdish.core.order.application.dto.OrderResult;
import kr.lastdish.core.order.application.dto.OrderWithStoreResult;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.store.application.dto.StoreQuerySnapshot;
import kr.lastdish.core.store.application.port.in.StoreQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryFacade {

  private final OrderService orderService;
  private final StoreQueryUseCase storeQueryUseCase;

  public OrderWithStoreResult getEachOrder(Long memberId, Long orderId) {
    OrderResult order = orderService.getEachOrder(memberId, orderId);
    return enrich(List.of(order)).getFirst();
  }

  public Page<OrderWithStoreResult> getMyOrders(
      Long memberId, OrderStatus status, Pageable pageable) {
    Page<OrderResult> orders = orderService.getMyOrders(memberId, status, pageable);
    List<OrderWithStoreResult> enriched = enrich(orders.getContent());
    Map<Long, OrderWithStoreResult> byOrderId =
        enriched.stream()
            .collect(Collectors.toMap(result -> result.order().orderId(), Function.identity()));
    return orders.map(order -> byOrderId.get(order.orderId()));
  }

  private List<OrderWithStoreResult> enrich(List<OrderResult> orders) {
    List<Long> storeIds = orders.stream().map(OrderResult::storeId).distinct().toList();
    Map<Long, String> storeNames =
        storeQueryUseCase.findActiveStores(storeIds).stream()
            .collect(Collectors.toMap(StoreQuerySnapshot::storeId, StoreQuerySnapshot::storeName));
    return orders.stream()
        .map(order -> new OrderWithStoreResult(order, storeNames.get(order.storeId())))
        .toList();
  }
}
