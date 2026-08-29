package kr.lastdish.common.outbox.domain;

/** 발행이 완료된 Outbox 이벤트를 배치 단위로 정리하는 저장소 계약입니다. */
public interface OutboxCleanupRepository {

  /** 오래 보관할 필요가 없는 PUBLISHED 이벤트를 최대 batchSize만큼 삭제합니다. */
  int deletePublishedBatch(int batchSize);
}
