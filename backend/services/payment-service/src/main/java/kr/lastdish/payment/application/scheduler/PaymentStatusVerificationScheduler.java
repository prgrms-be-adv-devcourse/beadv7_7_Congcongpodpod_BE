package kr.lastdish.payment.application.scheduler;

import kr.lastdish.payment.application.PaymentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusVerificationScheduler {

  private final PaymentFacade paymentFacade;

  @Scheduled(fixedDelayString = "${payment.processing-verify.interval-ms:300000}")
  public void verifyProcessingPayments() {
    paymentFacade.verifyProcessingPayments();
  }
}
