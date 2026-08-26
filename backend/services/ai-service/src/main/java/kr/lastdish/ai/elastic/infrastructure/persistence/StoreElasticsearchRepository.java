package kr.lastdish.ai.elastic.infrastructure.persistence;

import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StoreElasticsearchRepository
    extends ElasticsearchRepository<StoreDocument, Long> {}
