package kr.lastdish.ai.elastic.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.infrastructure.client.CoreInternalApiClient;
import kr.lastdish.ai.elastic.infrastructure.client.dto.InternalDishResponse;
import kr.lastdish.ai.elastic.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.ai.elastic.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.elastic.infrastructure.persistence.StoreElasticsearchRepository;
import kr.lastdish.ai.elastic.presentation.dto.TestStoreIndexRequest;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreIndexerService {

  private final StoreElasticsearchRepository repository;
  private final CoreInternalApiClient coreInternalApiClient;
  private final EmbeddingService embeddingService;
  private final ElasticsearchOperations elasticsearchOperations;

  private static final String STATUS_ONLY_EVENT = "STORE_STATUS_CHANGED";

  // 필드 배열 인덱스 고정 순서: 0=가게명, 1=메뉴명, 2=설명
  private static final int IDX_STORE_NAME = 0;
  private static final int IDX_DISH_NAME = 1;
  private static final int IDX_DESCRIPTION = 2;

  public void renewStoreIndex(Long storeId) {
    renewStoreIndex(storeId, null);
  }

  public void renewStoreIndex(Long storeId, String eventType) {
    if (storeId == null || storeId <= 0) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT);
    }

    StopWatch stopWatch = new StopWatch("StoreIndexRenewal-" + storeId);

    stopWatch.start("1. Core API 가게 정보 조회");
    var storeDataOpt = coreInternalApiClient.fetchStoreRenewalData(storeId);
    stopWatch.stop();

    storeDataOpt.ifPresentOrElse(
        response -> {
          stopWatch.start("2. 기존 문서 조회");
          StoreDocument existing = repository.findById(storeId).orElse(null);
          stopWatch.stop();

          stopWatch.start("3. 임베딩 및 Document 매핑");
          StoreDocument document = mapToDocument(response, existing, eventType);
          stopWatch.stop();

          stopWatch.start("4. ES 색인 저장 (Save)");
          repository.save(document);
          stopWatch.stop();

          log.info("Store 단건 색인 갱신 완료. storeId={}", storeId);
        },
        () -> deleteStoreIndex(storeId));
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

  public void syncUpdatedStores(Instant from, Instant to) {
    List<InternalStoreResponse> updatedStores =
        coreInternalApiClient.fetchStoresUpdatedWithin(from, to);

    if (updatedStores.isEmpty()) {
      return;
    }

    // 삭제된 매장은 tombstone으로 처리해 기존 색인을 제거한다.
    updatedStores.stream()
        .filter(InternalStoreResponse::deleted)
        .map(InternalStoreResponse::storeId)
        .forEach(this::deleteStoreIndex);

    List<InternalStoreResponse> activeStores =
        updatedStores.stream().filter(store -> !store.deleted()).toList();
    if (activeStores.isEmpty()) {
      return;
    }

    // [from, to] 구간의 활성 매장 전체를 대상으로 처리한다.
    // 일부만 선별하고 watermark가 to로 전진하면 나머지가 영영 누락되므로 limit을 두지 않는다.
    List<Long> storeIds = activeStores.stream().map(InternalStoreResponse::storeId).toList();
    Iterable<StoreDocument> existingDocs = repository.findAllById(storeIds);

    Map<Long, StoreDocument> existingMap =
        StreamSupport.stream(existingDocs.spliterator(), false)
            .collect(Collectors.toMap(StoreDocument::getStoreId, doc -> doc));

    // 폴링은 이벤트 타입 정보가 없으므로 필드별 텍스트 해시 비교로 재임베딩 여부 판단 후 매핑
    List<StoreDocument> documents =
        activeStores.stream()
            .map(res -> mapToDocument(res, existingMap.get(res.storeId()), null))
            .toList();

    repository.saveAll(documents);
    log.info("Polling 기반 Store 색인 동기화 완료. count={}", documents.size());
  }

  /** 가게명/메뉴명/설명 3개 필드의 임베딩 텍스트를 조립한다. 메뉴는 매장당 1개 고정을 전제로 한다. */
  private String[] buildFieldTexts(String storeName, InternalDishResponse dish) {
    String storeNameText = "가게: " + nullToEmpty(storeName);
    String dishNameText = dish != null ? "메뉴: " + nullToEmpty(dish.dishName()) : "";
    String descriptionText = dish != null ? nullToEmpty(dish.description()) : "";
    return new String[] {storeNameText, dishNameText, descriptionText};
  }

  private String[] buildFieldTexts(String storeName, TestStoreIndexRequest.DishRequest dish) {
    String storeNameText = "가게: " + nullToEmpty(storeName);
    String dishNameText = dish != null ? "메뉴: " + nullToEmpty(dish.dishName()) : "";
    String descriptionText = dish != null ? nullToEmpty(dish.description()) : "";
    return new String[] {storeNameText, dishNameText, descriptionText};
  }

  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private StoreDocument.DishItem mapToDishItem(InternalDishResponse d) {
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
        .pickupSpansMidnight(d.pickupStartTime().isAfter(d.pickupEndTime()))
        .build();
  }

  private StoreDocument.DishItem mapToDishItem(TestStoreIndexRequest.DishRequest d) {
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
        .pickupSpansMidnight(d.pickupStartTime().isAfter(d.pickupEndTime()))
        .build();
  }

  private StoreDocument mapToDocument(
      InternalStoreResponse res, StoreDocument existing, String eventType) {
    String[] texts = buildFieldTexts(res.storeName(), res.dish());

    List<StoreDocument.DishItem> dishItems =
        res.dish() != null ? List.of(mapToDishItem(res.dish())) : Collections.emptyList();

    FieldVectorResult vectors = resolveFieldVectors(texts, existing, eventType, res.storeId());

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
        .storeNameVector(vectors.vectors[IDX_STORE_NAME])
        .storeNameHash(vectors.hashes[IDX_STORE_NAME])
        .dishNameVector(vectors.vectors[IDX_DISH_NAME])
        .dishNameHash(vectors.hashes[IDX_DISH_NAME])
        .descriptionVector(vectors.vectors[IDX_DESCRIPTION])
        .descriptionHash(vectors.hashes[IDX_DESCRIPTION])
        .build();
  }

  private record FieldVectorResult(List<Float>[] vectors, String[] hashes) {}

  /**
   * 가게명/메뉴명/설명 3개 필드 중 텍스트가 바뀐 필드만 골라 임베딩 API를 한 번의 배치 호출로 재생성한다. 바뀌지 않은 필드는 기존 벡터를 그대로 재사용해서 호출
   * 비용/횟수를 최소화한다. 영업 상태만 바뀐 이벤트(STATUS_ONLY_EVENT)면 텍스트 자체가 안 바뀌었다고 보고, 기존 벡터가 유효한 필드는 전부
   * 재사용한다(필드별로 독립 판단 - 예: 설명만 비어있었다면 설명만 재시도).
   */
  @SuppressWarnings("unchecked")
  private FieldVectorResult resolveFieldVectors(
      String[] texts, StoreDocument existing, String eventType, Long storeId) {

    List<Float>[] existingVectors = new List[3];
    String[] existingHashes = new String[3];
    if (existing != null) {
      existingVectors[IDX_STORE_NAME] = existing.getStoreNameVector();
      existingVectors[IDX_DISH_NAME] = existing.getDishNameVector();
      existingVectors[IDX_DESCRIPTION] = existing.getDescriptionVector();
      existingHashes[IDX_STORE_NAME] = existing.getStoreNameHash();
      existingHashes[IDX_DISH_NAME] = existing.getDishNameHash();
      existingHashes[IDX_DESCRIPTION] = existing.getDescriptionHash();
    }

    boolean statusOnlyEvent = STATUS_ONLY_EVENT.equals(eventType);

    List<Float>[] resultVectors = new List[3];
    String[] resultHashes = new String[3];

    List<Integer> reembedIndices = new ArrayList<>();
    List<String> reembedTexts = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      boolean hasValidExisting = existingVectors[i] != null && !existingVectors[i].isEmpty();
      String newHash = hashText(texts[i]);

      if (statusOnlyEvent && hasValidExisting) {
        resultVectors[i] = existingVectors[i];
        resultHashes[i] = existingHashes[i];
        continue;
      }

      if (hasValidExisting && newHash.equals(existingHashes[i])) {
        // 해당 필드 텍스트 변경 없음 - 재임베딩 스킵
        resultVectors[i] = existingVectors[i];
        resultHashes[i] = existingHashes[i];
        continue;
      }

      // 텍스트가 바뀌었거나, 이전에 임베딩이 비어있었던 필드만 재임베딩 대상으로 모은다
      resultHashes[i] = newHash;
      reembedIndices.add(i);
      reembedTexts.add(texts[i]);
    }

    if (!reembedIndices.isEmpty()) {
      log.info("필드별 재임베딩 대상 필드 수={} (배치 1회 호출). storeId={}", reembedIndices.size(), storeId);
      List<List<Float>> newVectors = embeddingService.getEmbeddingBatch(reembedTexts);
      for (int i = 0; i < reembedIndices.size(); i++) {
        int fieldIndex = reembedIndices.get(i);
        resultVectors[fieldIndex] = i < newVectors.size() ? newVectors.get(i) : null;
      }
    }

    return new FieldVectorResult(resultVectors, resultHashes);
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

  public void indexTestStoresBulk(List<TestStoreIndexRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return;
    }
    for (TestStoreIndexRequest request : requests) {
      indexTestStore(request);
    }
  }

  public void indexTestStore(TestStoreIndexRequest req) {
    StopWatch stopWatch = new StopWatch("TestStoreSingleIndexing-" + req.storeId());
    stopWatch.start("1. 텍스트 조립 및 필드별 임베딩 (배치 1회 호출)");

    TestStoreIndexRequest.DishRequest dish =
        (req.dishes() != null && !req.dishes().isEmpty()) ? req.dishes().get(0) : null;
    String[] texts = buildFieldTexts(req.storeName(), dish);

    List<StoreDocument.DishItem> dishItems =
        dish == null ? Collections.emptyList() : List.of(mapToDishItem(dish));

    List<List<Float>> vectors = embeddingService.getEmbeddingBatch(List.of(texts));
    stopWatch.stop();

    stopWatch.start("2. ES Document 생성 및 Save");
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
            .storeNameVector(vectors.get(IDX_STORE_NAME))
            .storeNameHash(hashText(texts[IDX_STORE_NAME]))
            .dishNameVector(vectors.get(IDX_DISH_NAME))
            .dishNameHash(hashText(texts[IDX_DISH_NAME]))
            .descriptionVector(vectors.get(IDX_DESCRIPTION))
            .descriptionHash(hashText(texts[IDX_DESCRIPTION]))
            .build();

    repository.save(document);
    stopWatch.stop();
    log.info("[TEST] Store 색인 완료. storeId={}\n{}", req.storeId(), stopWatch.prettyPrint());
  }

  /** 가게명/메뉴명/설명 3개 벡터 필드 중 하나라도 없는 문서를 찾아 재시도한다 (기존 polling 구조 확장). */
  public void retryFailedEmbeddings() {
    NativeQuery query =
        NativeQuery.builder()
            .withQuery(
                q ->
                    q.bool(
                        b ->
                            b.should(
                                    s ->
                                        s.bool(
                                            sb ->
                                                sb.mustNot(
                                                    mn ->
                                                        mn.exists(
                                                            e -> e.field("storeNameVector")))))
                                .should(
                                    s ->
                                        s.bool(
                                            sb ->
                                                sb.mustNot(
                                                    mn ->
                                                        mn.exists(e -> e.field("dishNameVector")))))
                                .should(
                                    s ->
                                        s.bool(
                                            sb ->
                                                sb.mustNot(
                                                    mn ->
                                                        mn.exists(
                                                            e -> e.field("descriptionVector")))))
                                .minimumShouldMatch("1")))
            .withPageable(org.springframework.data.domain.PageRequest.of(0, 500))
            .build();

    SearchHits<StoreDocument> hits = elasticsearchOperations.search(query, StoreDocument.class);

    for (SearchHit<StoreDocument> hit : hits.getSearchHits()) {
      StoreDocument doc = hit.getContent();
      try {
        renewStoreIndex(doc.getStoreId());
      } catch (Exception e) {
        log.error("임베딩 재시도 중 개별 매장 처리 실패 - 다음 매장으로 계속 진행. storeId={}", doc.getStoreId(), e);
      }
    }

    log.info("임베딩 실패 재시도 스캔 완료. count={}", hits.getTotalHits());
  }

  public void ensureIndexExists() {
    IndexOperations indexOps = elasticsearchOperations.indexOps(StoreDocument.class);
    if (!indexOps.exists()) {
      indexOps.create();
      indexOps.putMapping(indexOps.createMapping());
      log.info("stores 인덱스를 StoreDocument 매핑 기준으로 생성했습니다.");
    } else {
      log.info("stores 인덱스가 이미 존재합니다. 초기화를 건너뜁니다.");
    }
  }

  public long countIndexedStores() {
    return repository.count();
  }
}
