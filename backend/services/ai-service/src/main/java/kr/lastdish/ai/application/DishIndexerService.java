package kr.lastdish.ai.application;

import java.util.List;
import kr.lastdish.ai.domain.document.DishDocument;
import kr.lastdish.ai.infrastructure.persistence.DishElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishIndexerService {

  private final DishElasticsearchRepository repository;

  // 단건 색인
  public void indexSingleDish(DishDocument newDoc) {
    repository
        .findById(newDoc.getDishId())
        .ifPresentOrElse(
            existingDoc -> {
              // 수신된 데이터의 버전이 더 높은 경우에만 Update
              if (newDoc.getVersion() == null
                  || existingDoc.getVersion() == null
                  || newDoc.getVersion() > existingDoc.getVersion()) {
                repository.save(newDoc);
              }
            },
            () -> repository.save(newDoc));
  }

  // saveAll을 사용하여 Bulk 단건 요청 방지
  public void bulkIndexDishes(List<DishDocument> documents) {
    if (!documents.isEmpty()) {
      repository.saveAll(documents);
    }
  }

  public void deleteDish(Long dishId) {
    repository.deleteById(dishId);
  }
}
