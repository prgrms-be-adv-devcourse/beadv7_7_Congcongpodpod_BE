package kr.lastdish.ai.infrastructure.persistence;

import kr.lastdish.ai.domain.document.DishDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DishElasticsearchRepository extends ElasticsearchRepository<DishDocument, Long> {}
