package kr.lastdish.ai.elastic.application;

import java.util.List;
import kr.lastdish.ai.elastic.infrastructure.persistence.StoreElasticsearchRepository;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreQueryService {

  private final StoreElasticsearchRepository repository;

  public List<StoreResponse> getAllStores(int page, int size) {
    // ES에 저장된 매장 전체를 페이징하여 조회
    return repository.findAll(PageRequest.of(page, size)).stream()
        .map(StoreResponse::from)
        .toList();
  }
}
