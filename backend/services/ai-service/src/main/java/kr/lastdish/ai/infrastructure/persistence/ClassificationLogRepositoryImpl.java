package kr.lastdish.ai.infrastructure.persistence;

import kr.lastdish.ai.domain.model.ClassificationLog;
import kr.lastdish.ai.domain.repository.ClassificationLogRepository;
import kr.lastdish.ai.infrastructure.persistence.entity.ClassificationLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@RequiredArgsConstructor
public class ClassificationLogRepositoryImpl implements ClassificationLogRepository {

  private final ClassificationLogJpaRepository jpaRepository;

  // AI 분류 로그 DB에 저장
  @Override
  public Mono<ClassificationLog> save(ClassificationLog log) {
    return Mono.fromCallable(
            () -> {
              ClassificationLogEntity entity =
                  new ClassificationLogEntity(
                      log.getImageUrl(),
                      log.getPredictedCategory(),
                      log.getConfidence(),
                      log.getExecutionTimeMs());
              jpaRepository.save(entity);
              return log;
            })
        // 블로킹 JPA 작업을 별도의 스레드 풀(boundedElastic)로 격리
        .subscribeOn(Schedulers.boundedElastic());
  }
}
