package kr.lastdish.core.payment.presentation;

import jakarta.validation.Valid;
import kr.lastdish.core.payment.application.PaymentFacade;
import kr.lastdish.core.payment.application.PaymentService;
import kr.lastdish.core.payment.application.dto.PaymentApproveRequest;
import kr.lastdish.core.payment.application.dto.PaymentApproveResponse;
import kr.lastdish.core.payment.application.dto.PaymentReadyRequest;
import kr.lastdish.core.payment.application.dto.PaymentReadyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final PaymentFacade paymentFacade;

  @PostMapping
  public ResponseEntity<PaymentReadyResponse> ready(
      @Valid @RequestBody PaymentReadyRequest request,
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    return ResponseEntity.ok(paymentService.readyPayment(memberId, request));
  }

  @PostMapping("/approve")
  public ResponseEntity<PaymentApproveResponse> approve(
      @Valid @RequestBody PaymentApproveRequest request) {
    return ResponseEntity.ok(
        paymentFacade.approve(request.paymentKey(), request.orderId(), request.amount()));
  }
}
