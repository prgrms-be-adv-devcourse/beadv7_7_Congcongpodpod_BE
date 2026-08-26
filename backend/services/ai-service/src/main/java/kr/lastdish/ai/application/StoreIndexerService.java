package kr.lastdish.ai.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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

          log.info("Store 단건 색인 갱신 완료. storeId={}\n{}", storeId, stopWatch.prettyPrint());
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

  public void syncUpdatedStores(Instant from, Instant to) {
    List<InternalStoreResponse> updatedStores =
        coreInternalApiClient.fetchStoresUpdatedWithin(from, to);

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

    boolean hasValidExistingVector =
        existing != null && existing.getVector() != null && !existing.getVector().isEmpty();

    if (STATUS_ONLY_EVENT.equals(eventType) && hasValidExistingVector) {
      // 영업 상태만 바뀌는 이벤트는 텍스트에 영향 없음 - 단, 기존 벡터가 유효할 때만 재사용
      vectorList = existing.getVector();
      embeddingHash = existing.getEmbeddingSourceHash();
      log.info("상태 변경 이벤트 - 재임베딩 스킵. storeId={}", res.storeId());
    } else {
      embeddingHash = hashText(embeddingText);

      if (hasValidExistingVector && embeddingHash.equals(existing.getEmbeddingSourceHash())) {
        // 텍스트 불변 + 기존 벡터가 실제로 존재할 때만 재사용
        vectorList = existing.getVector();
        log.info("임베딩 텍스트 변경 없음 - 재임베딩 스킵. storeId={}", res.storeId());
      } else {
        // 텍스트가 바뀌었거나, 이전에 임베딩이 비어있었던 경우 재시도
        vectorList = embeddingService.getEmbeddingList(embeddingText);
        if (existing == null) {
          log.info("신규 가게 최초 임베딩 수행. storeId={}", res.storeId());
        } else if (!hasValidExistingVector) {
          log.info("이전 임베딩 실패 이력 감지 - 재시도 수행. storeId={}", res.storeId());
        } else {
          log.info("임베딩 텍스트 변경 감지 - 재임베딩 수행. storeId={}", res.storeId());
        }
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
    stopWatch.start("1. 텍스트 조립");

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
    stopWatch.stop();

    stopWatch.start("2. OpenAI 임베딩 생성 (API 통신)");
    List<Float> vectorList = embeddingService.getEmbeddingList(embeddingText);
    stopWatch.stop();

    stopWatch.start("3. ES Document 생성 및 Save");
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
    stopWatch.stop();
    log.info("[TEST] Store 색인 완료. storeId={}\n{}", req.storeId(), stopWatch.prettyPrint());
  }

  public void retryFailedEmbeddings() {
    NativeQuery query =
        NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b.mustNot(mn -> mn.exists(e -> e.field("vector")))))
            .withPageable(org.springframework.data.domain.PageRequest.of(0, 500))
            .build();

    SearchHits<StoreDocument> hits = elasticsearchOperations.search(query, StoreDocument.class);

    for (SearchHit<StoreDocument> hit : hits.getSearchHits()) {
      StoreDocument doc = hit.getContent();
      renewStoreIndex(doc.getStoreId());
    }

    log.info("임베딩 실패 재시도 스캔 완료. count={}", hits.getTotalHits());
  }
}
