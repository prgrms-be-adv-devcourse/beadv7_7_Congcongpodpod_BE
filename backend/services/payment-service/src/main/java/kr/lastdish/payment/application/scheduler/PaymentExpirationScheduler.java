package kr.lastdish.payment.application.scheduler;

import kr.lastdish.payment.application.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

  private final PaymentService paymentService;

  @Scheduled(fixedDelayString = "${payment.ready-expire.interval-ms:300000}")
  public void expireReadyStatePayments() {
    int expiredCount = paymentService.expireReadyStatePayments();
    if (expiredCount > 0) {
      log.info("READY 상태로 방치된 결제 {}건을 EXPIRED 처리했습니다.", expiredCount);
    }
  }
}
