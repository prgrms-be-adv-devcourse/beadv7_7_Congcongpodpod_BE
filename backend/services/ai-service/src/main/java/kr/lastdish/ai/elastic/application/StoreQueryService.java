package kr.lastdish.ai.elastic.application;

import java.util.List;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreQueryService {

  private final ElasticsearchOperations elasticsearchOperations;

  public List<StoreResponse> getStoresByLocation(
      Double latitude, Double longitude, Double radiusKm, int page, int size) {

    GeoPoint userPoint = new GeoPoint(latitude, longitude);

    // location 필드 기준 radiusKm 반경 이내 조건 필터링
    Criteria criteria = new Criteria("location").within(userPoint, radiusKm.toString() + "km");

    CriteriaQuery query = new CriteriaQuery(criteria);
    query.setPageable(PageRequest.of(page, size));

    SearchHits<StoreDocument> searchHits =
        elasticsearchOperations.search(query, StoreDocument.class);

    return searchHits.stream().map(SearchHit::getContent).map(StoreResponse::from).toList();
  }
}
