-- core-service의 level 도메인 스키마입니다.

CREATE TABLE levels (
    level_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    dish_level VARCHAR(20) NOT NULL,
    purchase_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_levels_member_id UNIQUE (member_id)
);

CREATE TABLE level_history (
    level_history_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    old_level VARCHAR(20) NOT NULL,
    new_level VARCHAR(20) NOT NULL,
    purchase_count_at_change INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

