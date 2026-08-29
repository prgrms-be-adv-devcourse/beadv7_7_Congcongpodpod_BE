package kr.lastdish.common.inbox.domain;

import java.time.Instant;

/** 멱등성 보관기간이 지난 Inbox 완료 이벤트를 정리하는 저장소 계약입니다. */
public interface InboxCleanupRepository {

  /** 처리 완료 시각이 기준보다 오래된 PROCESSED 또는 SKIPPED 이벤트를 배치 삭제합니다. */
  int deleteCompletedBatch(Instant processedBefore, int batchSize);
}
