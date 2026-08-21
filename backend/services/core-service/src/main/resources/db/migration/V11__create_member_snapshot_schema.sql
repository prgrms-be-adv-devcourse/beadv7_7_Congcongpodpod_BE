CREATE TABLE public.member_snapshots (
    member_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    phone character varying(50) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT member_snapshots_pkey PRIMARY KEY (member_id)
);

-- MemberSnapshot projection의 생성·수정·삭제 이벤트가 하나의 aggregate version을 공유하도록
-- 기존 이벤트별 consumer 진행 상태를 공통 consumer로 합칩니다.
INSERT INTO public.inbox_aggregate_versions (
    consumer_id,
    aggregate_type,
    aggregate_id,
    last_processed_version,
    updated_at
)
SELECT
    'core-order-member-snapshot',
    aggregate_type,
    aggregate_id,
    MAX(last_processed_version),
    MAX(updated_at)
FROM public.inbox_aggregate_versions
WHERE consumer_id IN (
    'core-order-member-created',
    'core-order-member-updated',
    'core-order-member-deleted'
)
GROUP BY aggregate_type, aggregate_id
ON CONFLICT (consumer_id, aggregate_type, aggregate_id)
DO UPDATE SET
    last_processed_version = GREATEST(
        public.inbox_aggregate_versions.last_processed_version,
        EXCLUDED.last_processed_version
    ),
    updated_at = GREATEST(
        public.inbox_aggregate_versions.updated_at,
        EXCLUDED.updated_at
    );

DELETE FROM public.inbox_aggregate_versions
WHERE consumer_id IN (
    'core-order-member-created',
    'core-order-member-updated',
    'core-order-member-deleted'
);

INSERT INTO public.inbox_events (
    consumer_id,
    event_id,
    event_type,
    aggregate_type,
    aggregate_id,
    aggregate_version,
    schema_version,
    payload,
    status,
    retry_count,
    last_error,
    occurred_at,
    received_at,
    locked_at,
    processed_at
)
SELECT
    'core-order-member-snapshot',
    event_id,
    event_type,
    aggregate_type,
    aggregate_id,
    aggregate_version,
    schema_version,
    payload,
    status,
    retry_count,
    last_error,
    occurred_at,
    received_at,
    locked_at,
    processed_at
FROM public.inbox_events
WHERE consumer_id IN (
    'core-order-member-created',
    'core-order-member-updated',
    'core-order-member-deleted'
)
ON CONFLICT (consumer_id, event_id) DO NOTHING;

DELETE FROM public.inbox_events
WHERE consumer_id IN (
    'core-order-member-created',
    'core-order-member-updated',
    'core-order-member-deleted'
);
