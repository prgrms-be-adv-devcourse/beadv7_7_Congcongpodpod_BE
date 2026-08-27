package kr.lastdish.ai.infrastructure.persistence;

import kr.lastdish.ai.domain.document.StoreDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StoreElasticsearchRepository
    extends ElasticsearchRepository<StoreDocument, Long> {}
