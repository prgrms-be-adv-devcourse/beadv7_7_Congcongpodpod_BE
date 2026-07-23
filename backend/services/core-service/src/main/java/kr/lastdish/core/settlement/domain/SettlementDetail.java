package kr.lastdish.core.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "settlement_details",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlement_detail_order",
                        columnNames = "order_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_settlement_detail_settlement",
                        columnList = "settlement_id"
                )
        }
)
@Check(
        constraints = """
                sales_amount >= 0
                AND fee_rate >= 0
                AND fee_rate <= 1
                AND fee_amount >= 0
                AND settlement_amount >= 0
                AND settlement_amount = sales_amount - fee_amount
                """
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "settlement_id",
            nullable = false
    )
    private Long settlementId;

    @Column(
            name = "order_id",
            nullable = false
    )
    private Long orderId;

    @Column(
            name = "sales_amount",
            nullable = false
    )
    private long salesAmount;

    @Column(
            name = "fee_amount",
            nullable = false
    )
    private long feeAmount;

    @Column(
            name = "fee_rate",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal feeRate;

    @Column(
            name = "settlement_amount",
            nullable = false
    )
    private long settlementAmount;

    @Column(
            name = "order_completed_at",
            nullable = false
    )
    private LocalDateTime orderCompletedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public SettlementDetail(
            Long settlementId,
            Long orderId,
            long salesAmount,
            long feeAmount,
            BigDecimal feeRate,
            long settlementAmount,
            LocalDateTime orderCompletedAt
    ) {
        validate(
                settlementId,
                orderId,
                salesAmount,
                feeAmount,
                feeRate,
                settlementAmount,
                orderCompletedAt
        );

        this.settlementId = settlementId;
        this.orderId = orderId;
        this.salesAmount = salesAmount;
        this.feeAmount = feeAmount;
        this.feeRate = feeRate;
        this.settlementAmount = settlementAmount;
        this.orderCompletedAt = orderCompletedAt;
    }

    private void validate(
            Long settlementId,
            Long orderId,
            long salesAmount,
            long feeAmount,
            BigDecimal feeRate,
            long settlementAmount,
            LocalDateTime orderCompletedAt
    ) {
        if (settlementId == null) {
            throw new IllegalArgumentException("정산 ID는 필수입니다.");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }

        if (orderCompletedAt == null) {
            throw new IllegalArgumentException(
                    "주문 완료 시각은 필수입니다."
            );
        }

        if (salesAmount < 0
                || feeAmount < 0
                || settlementAmount < 0) {
            throw new IllegalArgumentException(
                    "정산 금액은 음수일 수 없습니다."
            );
        }

        if (feeRate == null
                || feeRate.compareTo(BigDecimal.ZERO) < 0
                || feeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "수수료율은 0 이상 1 이하여야 합니다."
            );
        }

        if (settlementAmount != salesAmount - feeAmount) {
            throw new IllegalArgumentException(
                    "주문 정산 금액 계산 결과가 일치하지 않습니다."
            );
        }
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
