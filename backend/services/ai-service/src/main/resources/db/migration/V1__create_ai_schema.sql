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
