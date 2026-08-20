-- core-service의 point 도메인 스키마입니다.

CREATE TABLE points (
                        point_id BIGSERIAL PRIMARY KEY,
                        member_id BIGINT NOT NULL,
                        balance NUMERIC(19,4) NOT NULL DEFAULT 0,
                        updated_at TIMESTAMP NOT NULL,
                        CONSTRAINT uq_points_member_id UNIQUE (member_id)
);

CREATE TABLE point_history (
                               point_history_id BIGSERIAL PRIMARY KEY,
                               member_id BIGINT NOT NULL,
                               order_id BIGINT,
                               type VARCHAR(20) NOT NULL,
                               amount NUMERIC(19,4) NOT NULL,
                               remaining_amount NUMERIC(19,4),
                               expires_at TIMESTAMP,
                               balance_after NUMERIC(19,4) NOT NULL,
                               created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_point_history_usable
    ON point_history (member_id, type, remaining_amount, expires_at);

ALTER TABLE points ADD CONSTRAINT chk_points_balance_min_zero CHECK (balance >= 0);
ALTER TABLE point_history ADD CONSTRAINT chk_point_history_remaining_min_zero CHECK (remaining_amount >= 0);
ALTER TABLE point_history ADD CONSTRAINT uq_point_history_order_type UNIQUE (order_id, type);