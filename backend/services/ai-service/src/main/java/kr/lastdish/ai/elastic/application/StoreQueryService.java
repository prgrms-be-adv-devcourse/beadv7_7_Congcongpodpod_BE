package kr.lastdish.ai.elastic.application;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreQueryService {

  private final ElasticsearchOperations elasticsearchOperations;
  private final Clock clock;

  public List<StoreResponse> getStoresByLocation(
      Double latitude,
      Double longitude,
      Double radiusKm,
      Boolean hasAvailableDish,
      int page,
      int size) {

    BoolQuery.Builder mainBool = new BoolQuery.Builder();

    // 1. 위치 기반 반경 필터 (공통)
    GeoLocation location = GeoLocation.of(l -> l.latlon(ll -> ll.lat(latitude).lon(longitude)));
    mainBool.filter(
        Query.of(
            q ->
                q.geoDistance(
                    g ->
                        g.field("location")
                            .distance(radiusKm + "km")
                            .distanceType(GeoDistanceType.Arc)
                            .location(location))));

    // 2. hasAvailableDish = true 인 경우 세부 조건 적용
    if (Boolean.TRUE.equals(hasAvailableDish)) {
      // 2-1. 매장 상태가 OPEN
      mainBool.filter(Query.of(q -> q.term(t -> t.field("status").value("OPEN"))));

      // 2-2. dishes 조건
      BoolQuery.Builder dishBool = new BoolQuery.Builder();

      // 재고 > 0
      dishBool.filter(
          Query.of(q -> q.range(r -> r.number(n -> n.field("dishes.stockQuantity").gt(0.0)))));

      // 상품 상태 ON_SALE
      dishBool.filter(Query.of(q -> q.term(t -> t.field("dishes.dishStatus").value("ON_SALE"))));

      // 현재 시각이 픽업 가능 시간 내인지 일반 구간과 자정 넘김 구간으로 나눠 확인한다.
      dishBool.filter(PickupTimeQueryFactory.currentlyAvailable(LocalTime.now(clock)));

      NestedQuery nestedQuery =
          NestedQuery.of(
              n ->
                  n.path("dishes")
                      .query(Query.of(dq -> dq.bool(dishBool.build())))
                      .scoreMode(ChildScoreMode.None));

      mainBool.filter(Query.of(q -> q.nested(nestedQuery)));
    }

    NativeQuery query =
        new NativeQueryBuilder()
            .withQuery(Query.of(q -> q.bool(mainBool.build())))
            .withPageable(PageRequest.of(page, size))
            .build();

    SearchHits<StoreDocument> searchHits =
        elasticsearchOperations.search(query, StoreDocument.class);

    return searchHits.stream().map(SearchHit::getContent).map(StoreResponse::from).toList();
  }
}
