package kr.lastdish.ai.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import kr.lastdish.ai.domain.document.StoreDocument;
import kr.lastdish.ai.infrastructure.client.CoreInternalApiClient;
import kr.lastdish.ai.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.ai.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.infrastructure.persistence.StoreElasticsearchRepository;
import kr.lastdish.ai.presentation.dto.TestStoreIndexRequest;
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
  private final EmbeddingService embeddingService;

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
    // N번 개별 findById 대신 findAllById로 묶어서 Bulk 조회
    List<Long> storeIds = updatedStores.stream().map(InternalStoreResponse::storeId).toList();
    Iterable<StoreDocument> existingDocs = repository.findAllById(storeIds);

    Map<Long, StoreDocument> existingMap =
        StreamSupport.stream(existingDocs.spliterator(), false)
            .collect(Collectors.toMap(StoreDocument::getStoreId, doc -> doc));

    List<StoreDocument> documents = updatedStores.stream().map(this::mapToDocument).toList();

    // ES Bulk Save
    repository.saveAll(documents);
    log.info("Polling 기반 Store 색인 동기화 완료. count={}", documents.size());
  }

  private StoreDocument mapToDocument(InternalStoreResponse res) {
    StringBuilder textBuilder = new StringBuilder();
    textBuilder.append("가게: ").append(res.storeName()).append(" ");

    List<StoreDocument.DishItem> dishItems =
        res.dishes() != null
            ? res.dishes().stream()
                .map(
                    d -> {
                      // 메뉴명 및 설명을 임베딩용 텍스트로 결합
                      textBuilder
                          .append("메뉴: ")
                          .append(d.dishName())
                          .append(" ")
                          .append(d.description() != null ? d.description() : "")
                          .append(" ");

                      return StoreDocument.DishItem.builder()
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
                          .build();
                    })
                .toList()
            : Collections.emptyList();

    // 결합된 전체 텍스트를 OpenAI 1536차원 실수 벡터로 전환
    List<Float> vectorList = embeddingService.getEmbeddingList(textBuilder.toString());

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
        .vector(vectorList)
        .build();
  }

  public void indexTestStore(TestStoreIndexRequest req) {
    StringBuilder textBuilder = new StringBuilder();
    textBuilder.append("가게: ").append(req.storeName()).append(" ");

    List<StoreDocument.DishItem> dishItems =
        req.dishes() == null
            ? Collections.emptyList()
            : req.dishes().stream()
                .map(
                    d -> {
                      textBuilder
                          .append("메뉴: ")
                          .append(d.dishName())
                          .append(" ")
                          .append(d.description() != null ? d.description() : "")
                          .append(" ");

                      return StoreDocument.DishItem.builder()
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
                          .build();
                    })
                .toList();

    List<Float> vectorList = embeddingService.getEmbeddingList(textBuilder.toString());

    StoreDocument document =
        StoreDocument.builder()
            .storeId(req.storeId())
            .storeName(req.storeName())
            .storeAddress(req.storeAddress())
            .openTime(req.openTime())
            .closeTime(req.closeTime())
            .status(req.status())
            .location(new GeoPoint(req.latitude(), req.longitude()))
            .category(req.category())
            .dishes(dishItems)
            .vector(vectorList)
            .build();

    repository.save(document);
    log.info("[TEST] Store 색인 완료. storeId={}", req.storeId());
  }
}
