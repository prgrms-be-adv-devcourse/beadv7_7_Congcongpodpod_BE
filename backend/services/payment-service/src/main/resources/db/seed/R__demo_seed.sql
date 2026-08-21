-- Mirrors the deterministic deposit-charge payments generated in core-service.
TRUNCATE TABLE public.payment_log, public.payments, public.outbox_events,
    public.inbox_events, public.inbox_aggregate_versions RESTART IDENTITY CASCADE;

WITH generated_orders AS (
    SELECT
        ((store_id + order_no - 2) % 300 + 1)::bigint AS member_id,
        (10000 + (store_id % 5) * 1000) * (1 + order_no % 3) AS total_price,
        order_no
    FROM generate_series(1, 300) AS stores(store_id)
    CROSS JOIN generate_series(1, 1000) AS orders(order_no)
), charges AS (
    SELECT
        member_id,
        sum(total_price) FILTER (WHERE order_no <= 850)
            + CASE WHEN member_id <= 150 THEN 10000000 ELSE 0 END AS charge_amount
    FROM generated_orders
    GROUP BY member_id
)
INSERT INTO public.payments (
    payment_id, member_id, amount, pg_provider, created_at, approved_at,
    approved_status, pg_transaction_id, merchant_order_id
)
SELECT
    member_id,
    member_id,
    charge_amount,
    'TOSS',
    current_timestamp - interval '9 years',
    current_timestamp - interval '9 years' + interval '1 minute',
    'APPROVED',
    'demo-payment-' || lpad(member_id::text, 3, '0'),
    'DEMO-CHARGE-' || lpad(member_id::text, 3, '0')
FROM charges;

SELECT setval('payments_payment_id_seq', 300, true);
