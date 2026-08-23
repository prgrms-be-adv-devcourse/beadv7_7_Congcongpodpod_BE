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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreIndexerService {

  private final StoreElasticsearchRepository repository;
  private final CoreInternalApiClient coreInternalApiClient;
  private final EmbeddingService embeddingService;

  public void renewStoreIndex(Long storeId) {
    renewStoreIndex(storeId, null);
  }

  public void renewStoreIndex(Long storeId, String eventType) {
    if (storeId == null || storeId <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    coreInternalApiClient
            .fetchStoreRenewalData(storeId)
            .ifPresentOrElse(
                    response -> {
                      StoreDocument existing = repository.findById(storeId).orElse(null);
                      StoreDocument document = mapToDocument(response, existing, eventType);
                      repository.save(document);
                      log.info("Store 색인 갱신 완료. storeId={}", storeId);
                    },
                    () -> {
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

    // 폴링은 이벤트 타입 정보가 없으므로, 텍스트 해시 비교로만 재임베딩 여부 판단
    List<StoreDocument> documents =
            updatedStores.stream()
                    .map(res -> mapToDocument(res, existingMap.get(res.storeId()), null))
                    .toList();

    repository.saveAll(documents);
    log.info("Polling 기반 Store 색인 동기화 완료. count={}", documents.size());
  }

  private static final String STATUS_ONLY_EVENT = "STORE_STATUS_CHANGED";

  private StoreDocument mapToDocument(
          InternalStoreResponse res, StoreDocument existing, String eventType) {
    StringBuilder textBuilder = new StringBuilder();
    textBuilder.append("가게: ").append(res.storeName()).append(" ");

    List<StoreDocument.DishItem> dishItems =
            res.dishes() != null
                    ? res.dishes().stream()
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
                    .toList()
                    : Collections.emptyList();

    String embeddingText = textBuilder.toString();
    List<Float> vectorList;
    String embeddingHash;

    if (STATUS_ONLY_EVENT.equals(eventType) && existing != null) {
      // 영업 상태만 바뀌는 이벤트는 텍스트에 영향을 줄 수 없으므로, 해시 비교 없이 바로 재사용
      vectorList = existing.getVector();
      embeddingHash = existing.getEmbeddingSourceHash();
      log.info("상태 변경 이벤트 - 재임베딩 스킵. storeId={}", res.storeId());
    } else {
      embeddingHash = hashText(embeddingText);

      if (existing != null && embeddingHash.equals(existing.getEmbeddingSourceHash())) {
        // 임베딩 대상 텍스트가 이전과 동일하면 기존 벡터 재사용
        vectorList = existing.getVector();
        log.info("임베딩 텍스트 변경 없음 - 재임베딩 스킵. storeId={}", res.storeId());
      } else {
        // 결합된 전체 텍스트를 OpenAI 1536차원 실수 벡터로 전환
        vectorList = embeddingService.getEmbeddingList(embeddingText);
        log.info("임베딩 텍스트 변경 감지 - 재임베딩 수행. storeId={}", res.storeId());
      }
    }

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
            .embeddingSourceHash(embeddingHash)
            .build();
  }

  private String hashText(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256은 JDK 표준 알고리즘이라 정상 환경에서는 발생하지 않음
      throw new IllegalStateException("해시 알고리즘을 사용할 수 없습니다.", e);
    }
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

    String embeddingText = textBuilder.toString();
    List<Float> vectorList = embeddingService.getEmbeddingList(embeddingText);

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
            .embeddingSourceHash(hashText(embeddingText))
            .build();

    repository.save(document);
    log.info("[TEST] Store 색인 완료. storeId={}", req.storeId());
  }
}
