package kr.lastdish.payment.presentation;

import jakarta.validation.Valid;
import kr.lastdish.payment.application.PaymentFacade;
import kr.lastdish.payment.application.PaymentService;
import kr.lastdish.payment.application.dto.PaymentApproveRequest;
import kr.lastdish.payment.application.dto.PaymentApproveResponse;
import kr.lastdish.payment.application.dto.PaymentReadyRequest;
import kr.lastdish.payment.application.dto.PaymentReadyResponse;
import kr.lastdish.payment.domain.ApprovedStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    PaymentApproveResponse response =
        paymentFacade.approve(request.paymentKey(), request.orderId(), request.amount());

    if (ApprovedStatus.PROCESSING.name().equals(response.approvedStatus())) {
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    return ResponseEntity.ok(response);
  }
}
