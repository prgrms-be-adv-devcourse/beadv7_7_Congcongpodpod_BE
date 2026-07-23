package kr.lastdish.core.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Entity
@Table(
        name = "settlements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlement_store_month",
                        columnNames = {
                                "store_id",
                                "settlement_month"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_settlement_store_created_at",
                        columnList = "store_id, created_at"
                ),
                @Index(
                        name = "idx_settlement_status",
                        columnList = "settlement_status"
                )
        }
)
@Check(
        constraints = """
                period_start < period_end
                AND total_order_count >= 0
                AND gross_amount >= 0
                AND fee_rate >= 0
                AND fee_rate <= 1
                AND fee_amount >= 0
                AND settlement_amount >= 0
                AND settlement_amount = gross_amount - fee_amount
                """
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "store_id",
            nullable = false
    )
    private Long storeId;

    @Convert(converter = YearMonthConverter.class)
    @Column(
            name = "settlement_month",
            nullable = false,
            length = 7
    )
    private YearMonth settlementMonth;

    @Column(
            name = "period_start",
            nullable = false
    )
    private LocalDateTime periodStart;

    @Column(
            name = "period_end",
            nullable = false
    )
    private LocalDateTime periodEnd;

    @Column(
            name = "total_order_count",
            nullable = false
    )
    private long totalOrderCount;

    @Column(
            name = "gross_amount",
            nullable = false
    )
    private long grossAmount;

    @Column(
            name = "fee_rate",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal feeRate;

    @Column(
            name = "fee_amount",
            nullable = false
    )
    private long feeAmount;

    @Column(
            name = "settlement_amount",
            nullable = false
    )
    private long settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "settlement_status",
            nullable = false,
            length = 20
    )
    private SettlementStatus settlementStatus;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;

    public Settlement(
            Long storeId,
            YearMonth settlementMonth,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            long totalOrderCount,
            long grossAmount,
            BigDecimal feeRate,
            long feeAmount,
            long settlementAmount
    ) {
        validate(
                storeId,
                settlementMonth,
                periodStart,
                periodEnd,
                totalOrderCount,
                grossAmount,
                feeRate,
                feeAmount,
                settlementAmount
        );

        this.storeId = storeId;
        this.settlementMonth = settlementMonth;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalOrderCount = totalOrderCount;
        this.grossAmount = grossAmount;
        this.feeRate = feeRate;
        this.feeAmount = feeAmount;
        this.settlementAmount = settlementAmount;
        this.settlementStatus = SettlementStatus.PROCESSING;
    }

    public void complete() {
        if (settlementStatus != SettlementStatus.PROCESSING) {
            throw new IllegalStateException(
                    "처리 중인 정산만 완료할 수 있습니다."
            );
        }

        this.settlementStatus = SettlementStatus.COMPLETED;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        if (settlementStatus != SettlementStatus.PROCESSING) {
            throw new IllegalStateException(
                    "처리 중인 정산만 실패 처리할 수 있습니다."
            );
        }

        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException(
                    "실패 사유는 필수입니다."
            );
        }

        this.settlementStatus = SettlementStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void restart() {
        if (settlementStatus != SettlementStatus.FAILED) {
            throw new IllegalStateException(
                    "실패한 정산만 재시작할 수 있습니다."
            );
        }

        this.settlementStatus = SettlementStatus.PROCESSING;
        this.failureReason = null;
    }

    private void validate(
            Long storeId,
            YearMonth settlementMonth,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            long totalOrderCount,
            long grossAmount,
            BigDecimal feeRate,
            long feeAmount,
            long settlementAmount
    ) {
        if (storeId == null) {
            throw new IllegalArgumentException("매장 ID는 필수입니다.");
        }

        if (settlementMonth == null) {
            throw new IllegalArgumentException("정산 대상 월은 필수입니다.");
        }

        if (periodStart == null || periodEnd == null) {
            throw new IllegalArgumentException("정산 기간은 필수입니다.");
        }

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                    "정산 시작 시각은 종료 시각보다 이전이어야 합니다."
            );
        }

        if (totalOrderCount < 0
                || grossAmount < 0
                || feeAmount < 0
                || settlementAmount < 0) {
            throw new IllegalArgumentException(
                    "정산 건수와 금액은 음수일 수 없습니다."
            );
        }

        if (feeRate == null
                || feeRate.compareTo(BigDecimal.ZERO) < 0
                || feeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "수수료율은 0 이상 1 이하여야 합니다."
            );
        }

        if (settlementAmount != grossAmount - feeAmount) {
            throw new IllegalArgumentException(
                    "정산 금액이 총 판매 금액과 수수료에 일치하지 않습니다."
            );
        }
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
