package kr.lastdish.ai.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

  public void renewStoreIndexWithVersion(Long storeId, Long eventVersion) {
    if (storeId == null || storeId <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    // 기존 ES 문서 조회
    Optional<StoreDocument> existingDocOpt = repository.findById(storeId);

    // 순서 검증 (이미 더 최신 버전의 이벤트가 반영되어 있다면 무시)
    if (existingDocOpt.isPresent()) {
      Long currentVersion = existingDocOpt.get().getVersion();
      if (currentVersion != null && eventVersion != null && eventVersion <= currentVersion) {
        log.info(
            "지연/중복 이벤트 무시. storeId={}, currentVersion={}, eventVersion={}",
            storeId,
            currentVersion,
            eventVersion);
        return;
      }
    }

    // Core API 호출
    coreInternalApiClient
        .fetchStoreRenewalData(storeId)
        .ifPresentOrElse(
            response -> {
              StoreDocument document = mapToDocument(response, eventVersion);
              // ES Overwrite - upsert
              repository.save(document);
              log.info("Store 색인 갱신 완료. storeId={}, version={}", storeId, eventVersion);
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
    // 기존 ES 문서의 version이 있다면 보존 처리
    List<StoreDocument> documents =
        updatedStores.stream()
            .map(
                res -> {
                  Long existingVersion =
                      repository
                          .findById(res.storeId())
                          .map(StoreDocument::getVersion)
                          .orElse(null);
                  return mapToDocument(res, existingVersion);
                })
            .toList();

    // ES Bulk Save
    repository.saveAll(documents);
    log.info("Polling 기반 Store 색인 동기화 완료. count={}", documents.size());
  }

  private StoreDocument mapToDocument(InternalStoreResponse res, Long version) {
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
        .version(version)
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
