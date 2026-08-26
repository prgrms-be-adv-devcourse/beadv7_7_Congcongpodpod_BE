package kr.lastdish.ai.elastic.application;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.domain.model.ParsedSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

  private final ElasticsearchOperations elasticsearchOperations;

  public List<SearchHit<StoreDocument>> searchStoresAndDishes(
      ParsedSearchCondition cond, GeoPoint userLocation, List<Float> queryVector) {
    NativeQuery query = buildQuery(cond, userLocation, queryVector);
    SearchHits<StoreDocument> searchHits =
        elasticsearchOperations.search(query, StoreDocument.class);
    return searchHits.getSearchHits();
  }

  private NativeQuery buildQuery(
      ParsedSearchCondition cond, GeoPoint userLocation, List<Float> queryVector) {
    BoolQuery mainBool = buildMainBoolQuery(cond, userLocation);

    NativeQueryBuilder builder =
        new NativeQueryBuilder()
            .withQuery(Query.of(q -> q.bool(mainBool)))
            .withPageable(PageRequest.of(0, 10));

    if (queryVector != null && !queryVector.isEmpty()) {
      List<Float> floatVector = new ArrayList<>(queryVector);

      KnnSearch knnSearch =
          KnnSearch.of(
              k ->
                  k.field("vector")
                      .queryVector(floatVector)
                      .k(10)
                      .numCandidates(100)
                      .filter(Query.of(fq -> fq.bool(mainBool))));

      builder.withKnnSearches(knnSearch);
    }

    return builder.build();
  }

  private BoolQuery buildMainBoolQuery(ParsedSearchCondition cond, GeoPoint userLocation) {
    BoolQuery.Builder mainBool = new BoolQuery.Builder();

    // 1. Store 루트 레벨 상태 필터
    mainBool.filter(Query.of(q -> q.term(t -> t.field("status").value("OPEN"))));

    // 2. 위치 기반 반경 거리 필터
    if (cond.maxDistanceKm() != null && userLocation != null) {
      GeoLocation location =
          GeoLocation.of(
              l -> l.latlon(ll -> ll.lat(userLocation.getLat()).lon(userLocation.getLon())));
      mainBool.filter(
          Query.of(
              q ->
                  q.geoDistance(
                      g ->
                          g.field("location")
                              .distance(cond.maxDistanceKm() + "km")
                              .distanceType(GeoDistanceType.Arc)
                              .location(location))));
    }

    // 3. Dish 기본 필터 조건 (재고 > 0 & 판매중 & 가격, 마감 지난 상품 제외)
    BoolQuery.Builder dishFilterBool = new BoolQuery.Builder();
    dishFilterBool.filter(
        Query.of(q -> q.range(r -> r.number(n -> n.field("dishes.stockQuantity").gt(0.0)))));
    dishFilterBool.filter(
        Query.of(q -> q.term(t -> t.field("dishes.dishStatus").value("ON_SALE"))));
    dishFilterBool.filter(
        Query.of(q -> q.range(r -> r.date(d -> d.field("dishes.pickupEndTime").gte("now")))));
    if (cond.maxPrice() != null) {
      dishFilterBool.filter(
          Query.of(
              q ->
                  q.range(
                      r ->
                          r.number(
                              n ->
                                  n.field("dishes.discountPrice")
                                      .lte(cond.maxPrice().doubleValue())))));
    }

    // 희망 픽업 시각(pickupDeadline): "이 시각 이전에 픽업하고 싶다"는 의도이므로
    // 픽업 시작 시각이 이미 그 이전이어야 함
    // 기존 pickupEndTime >= now 필터와 AND로 결합되어
    // "픽업 가능 구간이 [now, pickupDeadline]과 겹친다"를 표현
    if (cond.pickupDeadline() != null) {
      String deadline = cond.pickupDeadline().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      dishFilterBool.filter(
          Query.of(
              q -> q.range(r -> r.date(d -> d.field("dishes.pickupStartTime").lte(deadline)))));
    }

    // 카테고리 필터. dishes.category가 Keyword로 매핑되어 있어야 term 매칭이 정상 동작한다.
    if (cond.category() != null && !cond.category().isBlank()) {
      dishFilterBool.filter(
          Query.of(q -> q.term(t -> t.field("dishes.category").value(cond.category()))));
    }

    // 4. 키워드가 존재할 때만 should 검색 적용
    boolean hasQuery = cond.rawIntent() != null && !cond.rawIntent().isBlank();

    if (hasQuery) {
      // 4-1. 가게 이름 검색 (Should)
      MultiMatchQuery storeMatch =
          MultiMatchQuery.of(m -> m.fields("storeName^1.5").query(cond.rawIntent()));
      mainBool.should(Query.of(q -> q.multiMatch(storeMatch)));

      // 4-2. 메뉴 이름 및 설명 검색 (Should)
      dishFilterBool.should(
          Query.of(
              q ->
                  q.multiMatch(
                      m ->
                          m.fields("dishes.dishName^2.0", "dishes.description")
                              .query(cond.rawIntent()))));

      // 검색어가 있을 때는 should 중 1개 이상 매칭 필수
      mainBool.minimumShouldMatch("1");
    }

    // Dish Nested 필터를 mainBool의 filter절에 포함
    NestedQuery nestedQuery =
        NestedQuery.of(
            n ->
                n.path("dishes")
                    .query(Query.of(dq -> dq.bool(dishFilterBool.build())))
                    .scoreMode(ChildScoreMode.Max));

    if (hasQuery) {
      mainBool.should(Query.of(q -> q.nested(nestedQuery)));
    } else {
      mainBool.filter(Query.of(q -> q.nested(nestedQuery)));
    }

    return mainBool.build();
  }
}
