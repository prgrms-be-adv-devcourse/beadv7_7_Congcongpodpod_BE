-- Demo login password for every account: Demo1234!
-- BCrypt hash generated with cost 10.

INSERT INTO public.members (
    member_id,
    user_name,
    password,
    name,
    phone,
    email,
    role,
    created_at,
    updated_at,
    is_deleted,
    deleted_at
)
SELECT
    1000 + n,
    'demo_seller' || lpad(n::text, 2, '0'),
    '$2a$10$1O/2AplLN1llEcGlUrSwb.BL2C9CPD22ulJT1I346n5FQtoSkKyM6',
    '판매자' || lpad(n::text, 2, '0'),
    '010-1000-' || lpad(n::text, 4, '0'),
    'seller' || lpad(n::text, 2, '0') || '@lastdish.demo',
    'SELLER',
    timestamp '2026-06-01 09:00:00',
    timestamp '2026-06-01 09:00:00',
    false,
    null
FROM generate_series(1, 30) AS n;

INSERT INTO public.members (
    member_id,
    user_name,
    password,
    name,
    phone,
    email,
    role,
    created_at,
    updated_at,
    is_deleted,
    deleted_at
) VALUES (
    1100,
    'demo_member',
    '$2a$10$1O/2AplLN1llEcGlUrSwb.BL2C9CPD22ulJT1I346n5FQtoSkKyM6',
    '이구매',
    '010-0000-1100',
    'member@lastdish.demo',
    'MEMBER',
    timestamp '2026-06-01 09:00:00',
    timestamp '2026-06-01 09:00:00',
    false,
    null
);

SELECT setval(
    pg_get_serial_sequence('public.members', 'member_id'),
    (SELECT max(member_id) FROM public.members),
    true
);
