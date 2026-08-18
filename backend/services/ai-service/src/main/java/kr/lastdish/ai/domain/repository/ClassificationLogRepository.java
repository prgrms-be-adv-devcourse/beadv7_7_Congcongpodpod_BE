package kr.lastdish.ai.domain.repository;

import kr.lastdish.ai.domain.model.ClassificationLog;
import reactor.core.publisher.Mono;

public interface ClassificationLogRepository {
  Mono<ClassificationLog> save(ClassificationLog log);
}
