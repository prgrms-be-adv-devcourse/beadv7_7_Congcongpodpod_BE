-- Development/demo only. Enable with:
-- SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed

TRUNCATE TABLE
    public.notifications,
    public.refresh_tokens,
    public.members,
    public.outbox_events,
    public.inbox_events,
    public.inbox_aggregate_versions
    RESTART IDENTITY CASCADE;

WITH name_parts AS (
    SELECT
        ARRAY['김','이','박','최','정','강','조','윤','장','임','한','오','서','신','권','황','안','송','전','홍'] AS surnames,
        ARRAY['서준','서연','도윤','하윤','지호','지우','현우','민서','준서','수아',
              '예준','하은','시우','윤서','주원','채원','민준','유진','지훈','은우',
              '도현','서현','건우','예은','우진','소윤','준우','다은','현준','아린',
              '지환','나윤','태윤','가은','승현','지원','시윤','다온','재윤','유나'] AS given_names
)
INSERT INTO public.members (
    member_id, role, provider, provider_id, name, phone, user_name, email, password,
    is_deleted, deleted_at, created_at, updated_at
)
SELECT
    member_no,
    'SELLER',
    'LOCAL',
    NULL,
    surnames[((((member_no * 137) % 800) / array_length(given_names, 1))::integer % array_length(surnames, 1)) + 1]
        || given_names[(((member_no * 137) % 800)::integer % array_length(given_names, 1)) + 1],
    '010-0000-' || lpad(member_no::text, 4, '0'),
    'seller' || lpad(member_no::text, 3, '0'),
    'seller' || lpad(member_no::text, 3, '0') || '@seed.lastdish.kr',
    '$2y$10$Sy2yex48gzH5Ov71GjjSb.W4UvRBhrBCcAnLzkX5Y41ziWzuRVGvm',
    false,
    NULL,
    current_timestamp - interval '2 years',
    current_timestamp - interval '2 years'
FROM generate_series(1, 1000) AS members(member_no)
CROSS JOIN name_parts;

SELECT setval('members_member_id_seq', 1000, true);
