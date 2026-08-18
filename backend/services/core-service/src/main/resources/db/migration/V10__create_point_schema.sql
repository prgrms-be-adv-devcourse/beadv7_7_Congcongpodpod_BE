-- core-service의 point 도메인 스키마입니다.

CREATE TABLE points (
                        point_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        member_id BIGINT NOT NULL,
                        balance DECIMAL(19,4) NOT NULL DEFAULT 0,
                        updated_at DATETIME NOT NULL,
                        UNIQUE KEY uk_points_member_id (member_id)
);

CREATE TABLE point_history (
                               point_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               member_id BIGINT NOT NULL,
                               order_id BIGINT,
                               type VARCHAR(20) NOT NULL,
                               amount DECIMAL(19,4) NOT NULL,
                               remaining_amount DECIMAL(19,4),
                               expires_at DATETIME,
                               balance_after DECIMAL(19,4) NOT NULL,
                               created_at DATETIME NOT NULL
);

-- FIFO 소진 쿼리(findUsableEarnHistories) 성능을 위한 인덱스
CREATE INDEX idx_point_history_usable
    ON point_history (member_id, type, remaining_amount, expires_at);