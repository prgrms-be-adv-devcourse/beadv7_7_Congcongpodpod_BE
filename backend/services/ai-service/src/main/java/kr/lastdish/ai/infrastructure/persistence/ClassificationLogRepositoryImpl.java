package kr.lastdish.ai.infrastructure.persistence;

import kr.lastdish.ai.domain.model.ClassificationLog;
import kr.lastdish.ai.domain.repository.ClassificationLogRepository;
import kr.lastdish.ai.infrastructure.persistence.entity.ClassificationLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClassificationLogRepositoryImpl implements ClassificationLogRepository {

  private final ClassificationLogJpaRepository jpaRepository;

  // AI 분류 로그 DB에 저장
  @Override
  public ClassificationLog save(ClassificationLog log) {
    ClassificationLogEntity entity =
        new ClassificationLogEntity(
            log.getImageUrl(),
            log.getPredictedCategory(),
            log.getConfidence(),
            log.getExecutionTimeMs());

    jpaRepository.save(entity);
    return log;
  }
}
