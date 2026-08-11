package kr.lastdish.core.payment.domain.payment;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import kr.lastdish.common.api.crypto.EncryptConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_log_id")
  private Long id;

  @Column(name = "payment_id", nullable = false)
  private Long paymentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "pg_provider", nullable = false)
  private PgProvider pgProvider;

  @Convert(converter = EncryptConverter.class)
  @Column(name = "payment_method")
  private String paymentMethod;

  @Convert(converter = EncryptConverter.class)
  @Column(name = "masked_card_num")
  private String maskedCardNum;

  @Convert(converter = EncryptConverter.class)
  @Column(name = "card_company")
  private String cardCompany;

  @Column(name = "failed_code")
  private String failedCode;

  @Column(name = "failed_message")
  private String failedMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "log_type", nullable = false)
  private LogType logType;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "pg_result_code")
  private String pgResultCode;

  private PaymentLog(
      Long paymentId,
      PgProvider pgProvider,
      String paymentMethod,
      String maskedCardNum,
      String cardCompany,
      String failedCode,
      String failedMessage,
      LogType logType,
      Integer httpStatus,
      String pgResultCode) {
    this.paymentId = paymentId;
    this.pgProvider = pgProvider;
    this.paymentMethod = paymentMethod;
    this.maskedCardNum = maskedCardNum;
    this.cardCompany = cardCompany;
    this.failedCode = failedCode;
    this.failedMessage = failedMessage;
    this.logType = logType;
    this.httpStatus = httpStatus;
    this.pgResultCode = pgResultCode;
    this.createdAt = LocalDateTime.now();
  }

  // PG사 요청(REQUEST) 로그 생성
  public static PaymentLog createRequestLog(Long paymentId, PgProvider pgProvider) {
    return new PaymentLog(
        paymentId, pgProvider, null, null, null, null, null, LogType.REQUEST, null, null);
  }

  // PG사 응답(RESPONSE) 로그 생성
  public static PaymentLog createResponseLog(
      Long paymentId,
      PgProvider pgProvider,
      String paymentMethod,
      String maskedCardNum,
      String cardCompany,
      String failedCode,
      String failedMessage,
      Integer httpStatus,
      String pgResultCode) {
    return new PaymentLog(
        paymentId,
        pgProvider,
        paymentMethod,
        maskedCardNum,
        cardCompany,
        failedCode,
        failedMessage,
        LogType.RESPONSE,
        httpStatus,
        pgResultCode);
  }
}
