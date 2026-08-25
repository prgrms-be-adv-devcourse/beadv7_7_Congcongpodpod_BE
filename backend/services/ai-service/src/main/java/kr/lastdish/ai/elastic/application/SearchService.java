package kr.lastdish.ai.elastic.application;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
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

    // 1. Store 상태 필터
    TermQuery statusQuery = TermQuery.of(t -> t.field("status").value("OPEN"));
    mainBool.filter(Query.of(q -> q.term(statusQuery)));

    // 2. 위치 기반 반경 거리 필터
    if (cond.maxDistanceKm() != null && userLocation != null) {
      GeoLocation location =
          GeoLocation.of(
              l -> l.latlon(ll -> ll.lat(userLocation.getLat()).lon(userLocation.getLon())));

      GeoDistanceQuery geoDistanceQuery =
          GeoDistanceQuery.of(
              g ->
                  g.field("location")
                      .distance(cond.maxDistanceKm() + "km")
                      .distanceType(GeoDistanceType.Arc)
                      .location(location));

      mainBool.filter(Query.of(q -> q.geoDistance(geoDistanceQuery)));
    }

    // 3. Dish 레벨 필터 & 키워드 검색
    BoolQuery.Builder dishBool = new BoolQuery.Builder();

    RangeQuery stockQuery =
        RangeQuery.of(r -> r.number(n -> n.field("dishes.stockQuantity").gt(0.0)));
    dishBool.filter(Query.of(q -> q.range(stockQuery)));

    TermQuery sellingQuery = TermQuery.of(t -> t.field("dishes.dishStatus").value("ON_SALE"));
    dishBool.filter(Query.of(q -> q.term(sellingQuery)));

    if (cond.maxPrice() != null) {
      RangeQuery priceQuery =
          RangeQuery.of(
              r ->
                  r.number(
                      n -> n.field("dishes.discountPrice").lte(cond.maxPrice().doubleValue())));
      dishBool.filter(Query.of(q -> q.range(priceQuery)));
    }

    if (cond.rawIntent() != null && !cond.rawIntent().isBlank()) {
      MultiMatchQuery multiMatchQuery =
          MultiMatchQuery.of(
              m ->
                  m.fields("storeName^1.5", "dishes.dishName^2.0", "dishes.description")
                      .query(cond.rawIntent()));
      dishBool.should(Query.of(q -> q.multiMatch(multiMatchQuery)));
    }

    NestedQuery nestedQuery =
        NestedQuery.of(
            n ->
                n.path("dishes")
                    .query(Query.of(dq -> dq.bool(dishBool.build())))
                    .scoreMode(ChildScoreMode.Max));
    mainBool.must(Query.of(q -> q.nested(nestedQuery)));

    return mainBool.build();
  }
}
