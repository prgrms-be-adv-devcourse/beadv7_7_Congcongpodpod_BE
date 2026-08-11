CREATE TEMPORARY TABLE demo_catalog (
    number integer PRIMARY KEY,
    store_name varchar(255) NOT NULL,
    category varchar(255) NOT NULL,
    dish_name varchar(255) NOT NULL,
    description varchar(255) NOT NULL,
    dish_price numeric(38, 2) NOT NULL,
    discount_price numeric(38, 2) NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_catalog (
    number, store_name, category, dish_name, description, dish_price, discount_price
) VALUES
    (1,  '강남 치킨마켓',   'CHICKEN',       '순살 치킨',       '바삭하게 튀긴 순살 치킨',       18000, 13000),
    (2,  '강남반점',        'CHINESE',       '짜장면',          '춘장 소스와 돼지고기 짜장면',     10000, 7000),
    (3,  '강남 분식창고',   'BUNSIK',        '떡볶이',          '매콤달콤한 쌀떡볶이',             9000, 6000),
    (4,  '서울 한상',       'KOREAN',        '제육 도시락',     '매콤한 제육볶음 도시락',         12000, 8900),
    (5,  '따뜻한 찜탕',     'SOUP_STEW',     '김치찌개',        '돼지고기가 들어간 김치찌개',     10000, 7500),
    (6,  '강남 돈카츠',     'CUTLET_SUSHI',  '등심 돈카츠',     '두툼한 등심 돈카츠',             14000, 10000),
    (7,  '오늘의 피자',     'PIZZA',         '페퍼로니 피자',   '페퍼로니가 듬뿍 올라간 피자',   22000, 15000),
    (8,  '라스트 카페',     'CAFE_DESSERT',  '크로플',          '바삭한 크루아상 와플',            9000, 6000),
    (9,  '강남 버거샵',     'FAST_FOOD',     '불고기 버거',     '달콤한 불고기 소스 버거',        10000, 7000),
    (10, '장인의 족발',     'JOKBAL_BOSSAM', '족발 도시락',     '부드러운 족발과 밥 한상',        16000, 11000),
    (11, '강남 고기상회',   'MEAT',          '소불고기',        '간장 양념 소불고기',             17000, 12000),
    (12, '밤의 식탁',       'LATE_NIGHT',    '닭발',            '매운 양념 무뼈 닭발',            15000, 10000),
    (13, '강남 키친',       'WESTERN',       '토마토 파스타',   '진한 토마토소스 파스타',         15000, 10000),
    (14, '아시아 테이블',   'ASIAN',         '쌀국수',          '담백한 소고기 쌀국수',           12000, 8500),
    (15, '한끼 도시락',     'LUNCH_BOX',     '오늘의 도시락',   '매일 달라지는 균형 잡힌 도시락', 11000, 8000),
    (16, '역삼 치킨마켓',   'CHICKEN',       '양념 치킨',       '매콤달콤한 양념 치킨',           19000, 14000),
    (17, '역삼 분식창고',   'BUNSIK',        '순대 세트',       '순대와 내장 모둠 세트',          10000, 7000),
    (18, '역삼 한상',       'KOREAN',        '불고기 도시락',   '소불고기와 반찬 도시락',         14000, 9900),
    (19, '역삼 카페',       'CAFE_DESSERT',  '조각 케이크',     '부드러운 생크림 조각 케이크',    9000, 6500),
    (20, '역삼 버거샵',     'FAST_FOOD',     '치킨 버거',       '바삭한 치킨 패티 버거',          11000, 7500),
    (21, '논현 중화요리',   'CHINESE',       '볶음밥',          '고슬고슬한 계란 볶음밥',         11000, 7500),
    (22, '논현 찜탕',       'SOUP_STEW',     '부대찌개',        '햄과 소시지가 듬뿍 든 부대찌개', 13000, 9000),
    (23, '논현 돈카츠',     'CUTLET_SUSHI',  '치즈 돈카츠',     '치즈가 가득한 돈카츠',           16000, 11000),
    (24, '논현 피자마켓',   'PIZZA',         '고구마 피자',     '달콤한 고구마 무스 피자',       24000, 16000),
    (25, '논현 족발상회',   'JOKBAL_BOSSAM', '보쌈 도시락',     '담백한 보쌈과 밥 한상',          17000, 12000),
    (26, '논현 고기식당',   'MEAT',          '돼지불백',        '불향 가득한 돼지불백',           14000, 9500),
    (27, '논현 야식창고',   'LATE_NIGHT',    '오돌뼈',          '매콤하게 볶은 오돌뼈',           15000, 10000),
    (28, '논현 파스타',     'WESTERN',       '크림 파스타',     '고소한 크림소스 파스타',         16000, 11000),
    (29, '논현 아시안키친', 'ASIAN',         '팟타이',          '새콤달콤한 태국식 볶음면',       13000, 9000),
    (30, '논현 도시락',     'LUNCH_BOX',     '닭갈비 도시락',   '매콤한 닭갈비 도시락',           13000, 9000);

INSERT INTO public.stores (
    store_id,
    member_id,
    store_name,
    business_number,
    store_address,
    store_phone,
    open_time,
    close_time,
    status,
    latitude,
    longitude,
    is_deleted,
    category
)
SELECT
    2000 + number,
    1000 + number,
    store_name,
    '900-00-' || lpad(number::text, 5, '0'),
    '서울특별시 강남구 테헤란로 ' || (100 + number)::text,
    '02-0000-' || lpad(number::text, 4, '0'),
    time '09:00:00',
    time '23:00:00',
    'OPEN',
    37.4800000 + (((number - 1) / 6) * 0.0100000),
    127.0100000 + (((number - 1) % 6) * 0.0080000),
    false,
    category
FROM demo_catalog;

INSERT INTO public.store_holidays (holiday_id, store_id, day_of_week)
SELECT 2200 + number, 2000 + number, 'SUNDAY'
FROM demo_catalog;

INSERT INTO public.store_payout_accounts (
    payout_account_id,
    store_id,
    bank_name,
    account_number,
    account_holder,
    active,
    updated_at,
    is_deleted
)
SELECT
    2100 + number,
    2000 + number,
    '국민은행',
    '000000-00-' || lpad(number::text, 6, '0'),
    '판매자' || lpad(number::text, 2, '0'),
    true,
    timestamp '2026-06-01 09:00:00',
    false
FROM demo_catalog;

INSERT INTO public.dishes (
    id,
    store_id,
    dish_name,
    description,
    dish_price,
    discount_price,
    stock_quantity,
    dish_status,
    thumbnail_url,
    pickup_start_time,
    pickup_end_time,
    registered_at,
    is_deleted,
    event_version
)
SELECT
    3000 + number,
    2000 + number,
    dish_name,
    description,
    dish_price,
    discount_price,
    100,
    'ON_SALE',
    null,
    time '18:00:00',
    time '21:00:00',
    timestamp '2026-06-01 09:00:00',
    false,
    0
FROM demo_catalog;

INSERT INTO public.deposits (
    deposit_id, member_id, balance, updated_at
) VALUES (
    4001, 1100, 10000000.0000, timestamp '2026-06-01 09:00:00'
);

INSERT INTO public.deposit_history (
    deposit_history_id,
    member_id,
    order_id,
    payment_id,
    type,
    amount,
    balance_after,
    created_at
) VALUES (
    4101,
    1100,
    null,
    null,
    'CHARGE',
    10000000.0000,
    10000000.0000,
    timestamp '2026-06-01 09:00:00'
);

INSERT INTO public.carts (id, member_id)
VALUES (5001, 1100);

INSERT INTO public.orders (
    id,
    member_id,
    store_id,
    dish_id,
    dish_name,
    quantity,
    unit_price,
    total_price,
    status,
    payment_status,
    member_name,
    phone,
    pickup_code,
    pickup_start_at,
    pickup_end_at,
    pickup_deadline,
    cancel_reason,
    reject_reason,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    6001 + (store_number * 100) + (order_number - 1),
    1100,
    2001 + store_number,
    d.id,
    d.dish_name,
    1 + (order_number % 3),
    coalesce(d.discount_price, d.dish_price),
    coalesce(d.discount_price, d.dish_price) * (1 + (order_number % 3)),
    CASE WHEN order_number % 10 = 0 THEN 'NO_SHOW' ELSE 'PICKED_UP' END,
    'COMPLETED',
    '이구매',
    '010-0000-1100',
    'DEMO-' || lpad((6001 + (store_number * 100) + (order_number - 1))::text, 4, '0'),
    d.pickup_start_time,
    d.pickup_end_time,
    (timestamp '2026-06-01 10:00:00'
        + (((order_number - 1) % 30) * interval '1 day'))::date
        + d.pickup_end_time,
    null,
    null,
    timestamp '2026-06-01 10:00:00'
        + (((order_number - 1) % 30) * interval '1 day'),
    timestamp '2026-06-01 18:00:00'
        + (((order_number - 1) % 30) * interval '1 day'),
    false
FROM generate_series(0, 29) AS store_number
CROSS JOIN generate_series(1, 100) AS order_number
JOIN public.dishes d ON d.id = 3001 + store_number;

INSERT INTO public.payments (
    payment_id,
    member_id,
    amount,
    pg_provider,
    created_at,
    approved_at,
    approved_status,
    pg_transaction_id,
    merchant_order_id
)
SELECT
    4000 + id,
    member_id,
    total_price,
    'TOSS',
    created_at,
    updated_at,
    'APPROVED',
    'DEMO-TX-' || id::text,
    'DEMO-ORDER-' || id::text
FROM public.orders
WHERE id BETWEEN 6001 AND 9000;

SELECT setval(pg_get_serial_sequence('public.stores', 'store_id'), 2030, true);
SELECT setval(pg_get_serial_sequence('public.store_holidays', 'holiday_id'), 2230, true);
SELECT setval(pg_get_serial_sequence('public.store_payout_accounts', 'payout_account_id'), 2130, true);
SELECT setval(pg_get_serial_sequence('public.dishes', 'id'), 3030, true);
SELECT setval(pg_get_serial_sequence('public.deposits', 'deposit_id'), 4001, true);
SELECT setval(pg_get_serial_sequence('public.deposit_history', 'deposit_history_id'), 4101, true);
SELECT setval(pg_get_serial_sequence('public.carts', 'id'), 5001, true);
SELECT setval(pg_get_serial_sequence('public.orders', 'id'), 9000, true);
SELECT setval(pg_get_serial_sequence('public.payments', 'payment_id'), 13000, true);
