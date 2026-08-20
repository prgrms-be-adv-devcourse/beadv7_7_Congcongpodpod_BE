package kr.lastdish.core.settlement.infrastructure;

import kr.lastdish.core.settlement.domain.SettlementDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JdbcSettlementDetailRepository {
    private static final int BATCH_SIZE = 1_000;

    private static final String INSERT_SQL = """
        INSERT INTO settlement_details (
            settlement_id,
            order_id,
            sales_amount,
            fee_amount,
            fee_rate,
            settlement_amount,
            order_completed_at,
            created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsert(List<SettlementDetail> settlementDetails) {
        if (settlementDetails == null || settlementDetails.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                settlementDetails,
                BATCH_SIZE,
                (preparedStatement, detail) -> {
                    preparedStatement.setLong(1, detail.getSettlementId());
                    preparedStatement.setLong(2, detail.getOrderId());
                    preparedStatement.setLong(3, detail.getSalesAmount());
                    preparedStatement.setLong(4, detail.getFeeAmount());
                    preparedStatement.setBigDecimal(5, detail.getFeeRate());
                    preparedStatement.setLong(6, detail.getSettlementAmount());
                    preparedStatement.setTimestamp(
                            7,
                            Timestamp.valueOf(detail.getOrderCompletedAt())
                    );
                    preparedStatement.setTimestamp(8, Timestamp.valueOf(now));
                }
        );
    }
}
