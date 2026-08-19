package kr.lastdish.ai.domain.repository;

import kr.lastdish.ai.domain.model.ClassificationLog;

public interface ClassificationLogRepository {
  ClassificationLog save(ClassificationLog log);
}
