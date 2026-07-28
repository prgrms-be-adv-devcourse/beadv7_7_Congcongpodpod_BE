/// 매장 카테고리(백엔드 `Category` enum, `store.domain` 소유 — ADR 017). 서버는 이 값 이름
/// 그대로(영문 enum명)를 JSON으로 주고받는다 — `Store.category`도 이 문자열 그대로 들고 있는다.
///
/// 한글 표시명은 서버가 안 내려줘서(`getDisplayName()`은 백엔드 내부에서만 쓰임) 여기서
/// 직접 매핑해 화면(카테고리 필터 칩 등)에서 쓴다.
const List<String> categoryValues = [
  'CHICKEN',
  'CHINESE',
  'BUNSIK',
  'KOREAN',
  'SOUP_STEW',
  'CUTLET_SUSHI',
  'PIZZA',
  'CAFE_DESSERT',
  'FAST_FOOD',
  'JOKBAL_BOSSAM',
  'MEAT',
  'LATE_NIGHT',
  'WESTERN',
  'ASIAN',
  'LUNCH_BOX',
];

const Map<String, String> categoryDisplayNames = {
  'CHICKEN': '치킨',
  'CHINESE': '중식',
  'BUNSIK': '분식',
  'KOREAN': '한식',
  'SOUP_STEW': '찜·탕',
  'CUTLET_SUSHI': '돈까스·회',
  'PIZZA': '피자',
  'CAFE_DESSERT': '카페·디저트',
  'FAST_FOOD': '패스트푸드',
  'JOKBAL_BOSSAM': '족발·보쌈',
  'MEAT': '고기',
  'LATE_NIGHT': '야식',
  'WESTERN': '양식',
  'ASIAN': '아시안',
  'LUNCH_BOX': '도시락',
};

/// 서버 enum 값(예: `"CHICKEN"`)을 한글 표시명으로 바꾼다. 목록에 없는 값이 오면
/// (서버에 카테고리가 추가됐는데 앱이 아직 못 따라간 경우) 원래 값을 그대로 보여준다 —
/// 무단으로 숨기거나 에러를 던지지 않는다.
String categoryLabel(String value) => categoryDisplayNames[value] ?? value;
