package kr.lastdish.core.level.domain;

public interface LevelHistoryRepository {

  LevelHistory save(LevelHistory levelHistory);

  boolean existsByOrderId(Long orderId);
}
