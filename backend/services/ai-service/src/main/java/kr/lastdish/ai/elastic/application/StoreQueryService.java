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
import kr.lastdish.ai.elastic.domain.model.PickupFilter;
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
      PickupFilter pickupFilter,
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

    // 2. pickupFilter에 따라 매장 상태/상품 조건을 다르게 적용
    //    ALL은 상품 조건 없이 위치 필터만 적용
    if (pickupFilter == PickupFilter.NOW || pickupFilter == PickupFilter.TODAY) {
      // 2-1. 매장 상태가 OPEN
      mainBool.filter(Query.of(q -> q.term(t -> t.field("status").value("OPEN"))));

      // 2-2. dishes 조건
      BoolQuery.Builder dishBool = new BoolQuery.Builder();

      // 재고 > 0
      dishBool.filter(
          Query.of(q -> q.range(r -> r.number(n -> n.field("dishes.stockQuantity").gt(0.0)))));

      // 상품 상태 ON_SALE
      dishBool.filter(Query.of(q -> q.term(t -> t.field("dishes.dishStatus").value("ON_SALE"))));

      LocalTime now = LocalTime.now(clock);
      if (pickupFilter == PickupFilter.NOW) {
        // NOW: 현재 시각이 픽업 가능 구간 내인지 확인
        dishBool.filter(PickupTimeQueryFactory.currentlyAvailable(now));
      } else {
        // TODAY: 픽업 마감이 아직 지나지 않은 상품까지 포함
        dishBool.filter(PickupTimeQueryFactory.notExpired(now));
      }

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
