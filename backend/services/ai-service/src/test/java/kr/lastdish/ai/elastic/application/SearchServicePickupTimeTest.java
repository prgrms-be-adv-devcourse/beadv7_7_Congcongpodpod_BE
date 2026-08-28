package kr.lastdish.ai.elastic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.domain.model.ParsedSearchCondition;
import kr.lastdish.ai.elastic.domain.model.PickupFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.util.StringUtils;

class SearchServicePickupTimeTest {

  private static final Clock KST_23_CLOCK =
      Clock.fixed(Instant.parse("2026-08-22T14:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void 자정_넘김_구간은_23시와_01시_모두_시작_이후_또는_종료_이전이면_허용한다() {
    for (LocalTime currentTime : List.of(LocalTime.of(23, 0), LocalTime.of(1, 0))) {
      String query = PickupTimeQueryFactory.currentlyAvailable(currentTime).toString();

      assertThat(StringUtils.countOccurrencesOf(query, "pickupSpansMidnight")).isEqualTo(2);
      assertThat(StringUtils.countOccurrencesOf(query, "\"minimum_should_match\":\"1\""))
          .isEqualTo(2);
      assertThat(query).contains(currentTime.toString() + ":00");
    }
  }

  @Test
  void 자정을_넘는_픽업_구간을_별도_조건으로_검색한다() {
    ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    @SuppressWarnings("unchecked")
    SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
    when(operations.search(any(NativeQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);
    when(searchHits.getSearchHits()).thenReturn(List.of());
    SearchService searchService = new SearchService(operations, KST_23_CLOCK);

    searchService.searchStoresAndDishes(
        new ParsedSearchCondition(null, null, null, null, null, null), null, List.of());

    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations).search(queryCaptor.capture(), eq(StoreDocument.class));
    assertThat(queryCaptor.getValue().getQuery().toString())
        .contains("pickupSpansMidnight", "23:00:00");
  }

  @Test
  void 주변_매장_검색도_자정을_넘는_픽업_구간을_별도_조건으로_검색한다() {
    ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    @SuppressWarnings("unchecked")
    SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
    when(operations.search(any(NativeQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);
    when(searchHits.stream()).thenReturn(Stream.empty());
    StoreQueryService storeQueryService = new StoreQueryService(operations, KST_23_CLOCK);

    // hasAvailableDish=true는 "현재 픽업 가능 시간 내"를 의미했으므로 PickupFilter.NOW로 대응한다.
    storeQueryService.getStoresByLocation(37.5, 127.0, 3.0, PickupFilter.NOW, 0, 10);

    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations).search(queryCaptor.capture(), eq(StoreDocument.class));
    assertThat(queryCaptor.getValue().getQuery().toString())
        .contains("pickupSpansMidnight", "23:00:00");
  }

  @Test
  void 주변_매장_검색_TODAY_필터도_자정을_넘는_픽업_구간을_별도_조건으로_검색한다() {
    ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    @SuppressWarnings("unchecked")
    SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
    when(operations.search(any(NativeQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);
    when(searchHits.stream()).thenReturn(Stream.empty());
    StoreQueryService storeQueryService = new StoreQueryService(operations, KST_23_CLOCK);

    storeQueryService.getStoresByLocation(37.5, 127.0, 3.0, PickupFilter.TODAY, 0, 10);

    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations).search(queryCaptor.capture(), eq(StoreDocument.class));
    assertThat(queryCaptor.getValue().getQuery().toString())
        .contains("pickupSpansMidnight", "23:00:00");
  }

  @Test
  void 주변_매장_검색_ALL_필터는_dishes_중첩_조건_없이_위치_필터만_적용한다() {
    ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    @SuppressWarnings("unchecked")
    SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
    when(operations.search(any(NativeQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);
    when(searchHits.stream()).thenReturn(Stream.empty());
    StoreQueryService storeQueryService = new StoreQueryService(operations, KST_23_CLOCK);

    storeQueryService.getStoresByLocation(37.5, 127.0, 3.0, PickupFilter.ALL, 0, 10);

    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations).search(queryCaptor.capture(), eq(StoreDocument.class));
    String query = queryCaptor.getValue().getQuery().toString();
    assertThat(query).doesNotContain("pickupSpansMidnight", "dishes.stockQuantity");
  }

  @Test
  void 자정_이후_희망_픽업_시각도_자정_넘김_상품과_겹치는_구간으로_검색한다() {
    ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
    @SuppressWarnings("unchecked")
    SearchHits<StoreDocument> searchHits = mock(SearchHits.class);
    when(operations.search(any(NativeQuery.class), eq(StoreDocument.class))).thenReturn(searchHits);
    when(searchHits.getSearchHits()).thenReturn(List.of());
    SearchService searchService = new SearchService(operations, KST_23_CLOCK);

    searchService.searchStoresAndDishes(
        new ParsedSearchCondition(null, null, LocalTime.of(1, 0), null, null, null),
        null,
        List.of());

    ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
    verify(operations).search(queryCaptor.capture(), eq(StoreDocument.class));
    String query = queryCaptor.getValue().getQuery().toString();
    assertThat(StringUtils.countOccurrencesOf(query, "pickupSpansMidnight")).isEqualTo(4);
    assertThat(query).contains("23:00:00", "01:00:00");
  }
}
