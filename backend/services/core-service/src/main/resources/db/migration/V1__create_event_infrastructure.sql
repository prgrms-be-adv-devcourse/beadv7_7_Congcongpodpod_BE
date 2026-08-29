-- Core Service의 초기 이벤트 인프라와 presigned URL 발급 이력 스키마입니다.

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
    CONSTRAINT outbox_events_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_status_occurred_at
    ON public.outbox_events (status, occurred_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_outbox_published_at
    ON public.outbox_events (published_at)
    WHERE status = 'PUBLISHED';

CREATE TABLE public.inbox_events (
    consumer_id character varying(100) NOT NULL,
    event_id uuid NOT NULL,
    event_type character varying(100) NOT NULL,
    aggregate_type character varying(50) NOT NULL,
    aggregate_id bigint NOT NULL,
    aggregate_version bigint NOT NULL,
    schema_version integer NOT NULL,
    payload text NOT NULL,
    status character varying(20) NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    last_error character varying(1000),
    occurred_at timestamp(6) with time zone NOT NULL,
    received_at timestamp(6) with time zone NOT NULL,
    locked_at timestamp(6) with time zone,
    processed_at timestamp(6) with time zone,
    CONSTRAINT inbox_events_pkey PRIMARY KEY (consumer_id, event_id),
    CONSTRAINT inbox_events_status_check
        CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'SKIPPED', 'FAILED'))
);

CREATE INDEX idx_inbox_status_received_at
    ON public.inbox_events (status, received_at)
    WHERE status IN ('RECEIVED', 'PROCESSING');

CREATE INDEX idx_inbox_processed_at
    ON public.inbox_events (processed_at)
    WHERE status IN ('PROCESSED', 'SKIPPED');

CREATE INDEX idx_inbox_aggregate_version
    ON public.inbox_events (consumer_id, aggregate_type, aggregate_id, aggregate_version);

CREATE TABLE public.inbox_aggregate_versions (
    consumer_id character varying(100) NOT NULL,
    aggregate_type character varying(50) NOT NULL,
    aggregate_id bigint NOT NULL,
    last_processed_version bigint DEFAULT 0 NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT inbox_aggregate_versions_pkey
        PRIMARY KEY (consumer_id, aggregate_type, aggregate_id),
    CONSTRAINT inbox_aggregate_versions_non_negative_check
        CHECK (last_processed_version >= 0)
);

ALTER TABLE public.outbox_events SET (
    autovacuum_vacuum_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 100
);

ALTER TABLE public.inbox_events SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 100
);
