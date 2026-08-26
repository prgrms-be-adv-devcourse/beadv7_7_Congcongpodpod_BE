package kr.lastdish.ai.elastic.infrastructure.scheduler;

import kr.lastdish.ai.elastic.application.StoreIndexerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingRetryScheduler {

    private final StoreIndexerService storeIndexerService;

    @Scheduled(fixedRate = 60000)
    public void retryFailedEmbeddings() {
        try {
            storeIndexerService.retryFailedEmbeddings();
        } catch (Exception e) {
            log.error("임베딩 실패 재시도 스케줄러 실행 중 예외 발생", e);
        }
    }
}