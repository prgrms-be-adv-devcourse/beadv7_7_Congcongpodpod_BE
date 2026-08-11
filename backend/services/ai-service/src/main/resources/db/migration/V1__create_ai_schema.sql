-- AI 도메인 변경과 Kafka 발행을 한 트랜잭션으로 묶기 위한 전용 Outbox입니다.
-- 향후 AI 영속 모델은 같은 ai_db의 후속 Flyway migration에서 추가합니다.
CREATE TABLE public.outbox_events (
    retry_count integer NOT NULL,
    schema_version integer DEFAULT 1 NOT NULL,
    aggregate_id bigint NOT NULL,
    aggregate_version bigint DEFAULT 0 NOT NULL,
    locked_at timestamp(6) with time zone,
    occurred_at timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone,
    event_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    aggregate_type character varying(50) NOT NULL,
    event_type character varying(100) NOT NULL,
    last_error character varying(1000),
    payload text NOT NULL,
    CONSTRAINT outbox_events_pkey PRIMARY KEY (event_id),
    CONSTRAINT outbox_events_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_status_occurred_at
    ON public.outbox_events (status, occurred_at);

CREATE TABLE public.inbox_events (
                                     consumer_id VARCHAR(100) NOT NULL,
                                     event_id UUID NOT NULL,
                                     event_type VARCHAR(100) NOT NULL,
                                     aggregate_type VARCHAR(50) NOT NULL,
                                     aggregate_id BIGINT NOT NULL,
                                     aggregate_version BIGINT NOT NULL,
                                     schema_version INTEGER NOT NULL,
                                     payload TEXT NOT NULL,
                                     status VARCHAR(20) NOT NULL,
                                     retry_count INTEGER NOT NULL DEFAULT 0,
                                     last_error VARCHAR(1000),
                                     occurred_at TIMESTAMPTZ NOT NULL,
                                     received_at TIMESTAMPTZ NOT NULL,
                                     locked_at TIMESTAMPTZ,
                                     processed_at TIMESTAMPTZ,

                                     CONSTRAINT inbox_events_pkey
                                         PRIMARY KEY (consumer_id, event_id),

                                     CONSTRAINT inbox_events_status_check
                                         CHECK (status IN (
                                                           'RECEIVED',
                                                           'PROCESSING',
                                                           'PROCESSED',
                                                           'FAILED'
                                             ))
);

CREATE INDEX idx_inbox_status_received_at
    ON public.inbox_events (status, received_at);