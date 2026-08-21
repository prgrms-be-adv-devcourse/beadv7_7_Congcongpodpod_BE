package kr.lastdish.ai.application;

import java.util.Collections;
import java.util.List;
import kr.lastdish.ai.domain.document.StoreDocument;
import kr.lastdish.ai.infrastructure.client.CoreInternalApiClient;
import kr.lastdish.ai.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.ai.infrastructure.persistence.StoreElasticsearchRepository;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreIndexerService {

  private final StoreElasticsearchRepository repository;
  private final CoreInternalApiClient coreInternalApiClient;

  public void renewStoreIndex(Long storeId) {
    if (storeId == null || storeId <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    // Core API 호출
    coreInternalApiClient
        .fetchStoreRenewalData(storeId)
        .ifPresentOrElse(
            response -> {
              StoreDocument document = mapToDocument(response);
              // ES Overwrite - upsert
              repository.save(document);
              log.info("Store 색인 갱신 완료. storeId={}", storeId);
            },
            () -> {
              // 404일 때만 오직 삭제 실행
              deleteStoreIndex(storeId);
            });
  }

  public void deleteStoreIndex(Long storeId) {
    if (storeId == null || storeId <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    if (repository.existsById(storeId)) {
      repository.deleteById(storeId);
      log.info("Store 색인 삭제 완료. storeId={}", storeId);
    }
  }

  public void syncUpdatedStores(int minutes) {
    List<InternalStoreResponse> updatedStores =
        coreInternalApiClient.fetchStoresUpdatedWithin(minutes);

    if (updatedStores.isEmpty()) {
      return;
    }

    List<StoreDocument> documents = updatedStores.stream().map(this::mapToDocument).toList();

    // ES Bulk Save
    repository.saveAll(documents);
    log.info("Polling 기반 Store 색인 동기화 완료. count={}", documents.size());
  }

  private StoreDocument mapToDocument(InternalStoreResponse res) {
    List<StoreDocument.DishItem> dishItems =
        res.dishes() != null
            ? res.dishes().stream()
                .map(
                    d ->
                        StoreDocument.DishItem.builder()
                            .dishId(d.dishId())
                            .dishName(d.dishName())
                            .description(d.description())
                            .category(d.category())
                            .thumbnailUrl(d.thumbnailUrl())
                            .stockQuantity(d.stockQuantity())
                            .dishStatus(d.dishStatus())
                            .dishPrice(d.dishPrice())
                            .discountPrice(d.discountPrice())
                            .pickupStartTime(d.pickupStartTime())
                            .pickupEndTime(d.pickupEndTime())
                            .build())
                .toList()
            : Collections.emptyList();

    return StoreDocument.builder()
        .storeId(res.storeId())
        .storeName(res.storeName())
        .storeAddress(res.storeAddress())
        .openTime(res.openTime())
        .closeTime(res.closeTime())
        .status(res.status())
        .location(new GeoPoint(res.latitude().doubleValue(), res.longitude().doubleValue()))
        .category(res.category())
        .dishes(dishItems)
        .build();
  }
}
