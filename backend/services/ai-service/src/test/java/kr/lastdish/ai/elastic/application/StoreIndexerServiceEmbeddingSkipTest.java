package kr.lastdish.ai.elastic.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.infrastructure.client.CoreInternalApiClient;
import kr.lastdish.ai.elastic.infrastructure.client.dto.InternalDishResponse;
import kr.lastdish.ai.elastic.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.ai.elastic.infrastructure.embedding.EmbeddingService;
import kr.lastdish.ai.elastic.infrastructure.persistence.StoreElasticsearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreIndexerServiceEmbeddingSkipTest {

  @Mock private StoreElasticsearchRepository repository;
  @Mock private CoreInternalApiClient coreInternalApiClient;
  @Mock private EmbeddingService embeddingService;

  @InjectMocks private StoreIndexerService storeIndexerService;

  // StoreIndexerService.mapToDocument 의 텍스트 조립 로직과 동일하게 해시 생성
  private String calculateExpectedHash(InternalStoreResponse store) {
    StringBuilder textBuilder = new StringBuilder();
    textBuilder.append("가게: ").append(store.storeName()).append(" ");
    InternalDishResponse d = store.dish();
    if (d != null) {
      textBuilder
          .append("메뉴: ")
          .append(d.dishName())
          .append(" ")
          .append(d.description() != null ? d.description() : "")
          .append(" ");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(textBuilder.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private InternalDishResponse dish(
      Long dishId, String name, String desc, Long stock, BigDecimal price, BigDecimal discount) {
    return new InternalDishResponse(
        dishId,
        name,
        desc,
        "분식",
        null,
        stock,
        "ON_SALE",
        price,
        discount,
        LocalTime.MIN,
        LocalTime.MAX);
  }

  private InternalStoreResponse storeResponse(Long storeId, InternalDishResponse dish) {
    return storeResponse(storeId, dish, false);
  }

  private InternalStoreResponse storeResponse(
      Long storeId, InternalDishResponse dish, boolean deleted) {
    return new InternalStoreResponse(
        storeId,
        "맛있는 가게",
        "서울시 강남구",
        LocalTime.MIN,
        LocalTime.MAX,
        "OPEN",
        BigDecimal.valueOf(37.5),
        BigDecimal.valueOf(127.0),
        "KOREAN",
        dish,
        deleted);
  }

  @Test
  @DisplayName("폴링 응답이 삭제된 매장이면 기존 ES 문서를 삭제하고 다시 저장하지 않는다.")
  void syncUpdatedStores_deletedStore_deletesIndexWithoutUpsert() {
    Instant from = Instant.parse("2026-08-22T13:00:00Z");
    Instant to = Instant.parse("2026-08-22T13:01:00Z");
    Long storeId = 10L;
    given(coreInternalApiClient.fetchStoresUpdatedWithin(from, to))
        .willReturn(List.of(storeResponse(storeId, null, true)));
    given(repository.existsById(storeId)).willReturn(true);

    storeIndexerService.syncUpdatedStores(from, to);

    verify(repository).deleteById(storeId);
    verify(repository, never()).findAllById(ArgumentMatchers.any());
    verify(repository, never()).saveAll(ArgumentMatchers.any());
    verify(embeddingService, never()).getEmbeddingList(anyString());
  }

  @Test
  @DisplayName("폴링 배치에 삭제 매장과 활성 매장이 섞이면 각각 삭제와 저장을 수행한다.")
  void syncUpdatedStores_mixedStores_deletesAndUpsertsSeparately() {
    Instant from = Instant.parse("2026-08-22T13:00:00Z");
    Instant to = Instant.parse("2026-08-22T13:01:00Z");
    Long deletedStoreId = 10L;
    Long activeStoreId = 20L;
    given(coreInternalApiClient.fetchStoresUpdatedWithin(from, to))
        .willReturn(
            List.of(
                storeResponse(deletedStoreId, null, true),
                storeResponse(activeStoreId, null, false)));
    given(repository.existsById(deletedStoreId)).willReturn(true);
    given(repository.findAllById(List.of(activeStoreId))).willReturn(List.of());
    given(embeddingService.getEmbeddingList(anyString())).willReturn(List.of(0.1f));

    storeIndexerService.syncUpdatedStores(from, to);

    verify(repository).deleteById(deletedStoreId);
    verify(repository).findAllById(List.of(activeStoreId));
    verify(repository).saveAll(ArgumentMatchers.any());
  }


  @Test
  @DisplayName("Dish 수정으로 DISH_IS_CREATED가 재발행되더라도, 텍스트(메뉴명/설명)가 동일하면 OpenAI 임베딩 API를 호출하지 않는다.")
  void renewStoreIndex_sameTextOnDishReplace_skipsEmbedding() {
    // given
    Long storeId = 1L;
    InternalDishResponse newDish =
        dish(101L, "맛있는 떡볶이", "매콤달콤한 떡볶이", 5L, BigDecimal.valueOf(6000), BigDecimal.valueOf(4500));
    InternalStoreResponse storeResp = storeResponse(storeId, newDish);

    // 동적으로 동기화된 해시값 계산
    String expectedHash = calculateExpectedHash(storeResp);

    StoreDocument existingDoc =
        StoreDocument.builder()
            .storeId(storeId)
            .storeName("맛있는 가게")
            .vector(List.of(0.1f, 0.2f, 0.3f))
            .embeddingSourceHash(expectedHash)
            .build();

    given(repository.findById(storeId)).willReturn(Optional.of(existingDoc));
    given(coreInternalApiClient.fetchStoreRenewalData(storeId)).willReturn(Optional.of(storeResp));

    // when
    storeIndexerService.renewStoreIndex(storeId, "DISH_IS_CREATED");

    // then
    verify(embeddingService, never()).getEmbeddingList(anyString());
    verify(repository).save(ArgumentMatchers.any(StoreDocument.class));
  }

  @Test
  @DisplayName("신규 매장(기존 문서 없음)은 반드시 임베딩을 새로 생성한다.")
  void renewStoreIndex_newStore_generatesEmbedding() {
    // given
    Long storeId = 2L;
    given(repository.findById(storeId)).willReturn(Optional.empty());

    InternalDishResponse newDish =
        dish(200L, "김치찌개", "얼큰한 김치찌개", 10L, BigDecimal.valueOf(8000), BigDecimal.valueOf(7000));
    given(coreInternalApiClient.fetchStoreRenewalData(storeId))
        .willReturn(Optional.of(storeResponse(storeId, newDish)));
    given(embeddingService.getEmbeddingList(anyString())).willReturn(List.of(0.1f, 0.2f));

    // when
    storeIndexerService.renewStoreIndex(storeId, "STORE_CREATED");

    // then
    verify(embeddingService, times(1)).getEmbeddingList(anyString());
    verify(repository).save(ArgumentMatchers.any(StoreDocument.class));
  }

  @Test
  @DisplayName("메뉴명/설명이 실제로 바뀌면 텍스트 해시가 달라져 재임베딩이 수행된다.")
  void renewStoreIndex_textChanged_reEmbeds() {
    // given
    Long storeId = 3L;
    StoreDocument existingDoc =
        StoreDocument.builder()
            .storeId(storeId)
            .storeName("맛있는 가게")
            .vector(List.of(0.1f, 0.2f, 0.3f))
            .embeddingSourceHash("완전히다른예전해시값")
            .build();
    given(repository.findById(storeId)).willReturn(Optional.of(existingDoc));

    InternalDishResponse changedDish =
        dish(
            300L,
            "매운 떡볶이로 개명",
            "완전히 새로운 설명",
            5L,
            BigDecimal.valueOf(5000),
            BigDecimal.valueOf(4000));
    given(coreInternalApiClient.fetchStoreRenewalData(storeId))
        .willReturn(Optional.of(storeResponse(storeId, changedDish)));
    given(embeddingService.getEmbeddingList(anyString())).willReturn(List.of(0.4f, 0.5f));

    // when
    storeIndexerService.renewStoreIndex(storeId, "DISH_IS_UPDATED");

    // then
    verify(embeddingService, times(1)).getEmbeddingList(anyString());
  }

  @Test
  @DisplayName("STORE_STATUS_CHANGED 이벤트는 기존 벡터가 유효하면 해시 비교 없이 바로 재사용한다.")
  void renewStoreIndex_statusChangedEvent_reusesVectorWithoutHashCheck() {
    // given
    Long storeId = 4L;
    StoreDocument existingDoc =
        StoreDocument.builder()
            .storeId(storeId)
            .storeName("맛있는 가게")
            .vector(List.of(0.9f, 0.8f))
            .embeddingSourceHash("아무값이어도상관없음")
            .build();
    given(repository.findById(storeId)).willReturn(Optional.of(existingDoc));

    InternalDishResponse dish =
        dish(400L, "떡볶이", "설명", 5L, BigDecimal.valueOf(5000), BigDecimal.valueOf(4000));
    given(coreInternalApiClient.fetchStoreRenewalData(storeId))
        .willReturn(Optional.of(storeResponse(storeId, dish)));

    // when
    storeIndexerService.renewStoreIndex(storeId, "STORE_STATUS_CHANGED");

    // then
    verify(embeddingService, never()).getEmbeddingList(anyString());
  }

  @Test
  @DisplayName("기존 벡터가 비어있으면(이전 임베딩 실패 이력) 텍스트가 같아도 재임베딩을 시도한다.")
  void renewStoreIndex_previousEmbeddingFailed_retriesEvenIfTextUnchanged() {
    // given
    Long storeId = 5L;
    InternalDishResponse sameDish =
        dish(500L, "맛있는 떡볶이", "매콤달콤한 떡볶이", 5L, BigDecimal.valueOf(6000), BigDecimal.valueOf(4500));
    InternalStoreResponse storeResp = storeResponse(storeId, sameDish);

    String expectedHash = calculateExpectedHash(storeResp);

    StoreDocument existingDocWithEmptyVector =
        StoreDocument.builder()
            .storeId(storeId)
            .storeName("맛있는 가게")
            .vector(List.of()) // 이전 임베딩 실패로 빈 벡터
            .embeddingSourceHash(expectedHash)
            .build();
    given(repository.findById(storeId)).willReturn(Optional.of(existingDocWithEmptyVector));

    given(coreInternalApiClient.fetchStoreRenewalData(storeId)).willReturn(Optional.of(storeResp));
    given(embeddingService.getEmbeddingList(anyString())).willReturn(List.of(0.1f));

    // when
    storeIndexerService.renewStoreIndex(storeId, "DISH_IS_UPDATED");

    // then
    verify(embeddingService, times(1)).getEmbeddingList(anyString());
  }
}
