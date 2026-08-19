export const cartItem = { dishId: 3013, storeId: 2013, storeName: '강남 키친', name: '토마토 파스타', price: 10000, originalPrice: 15000, quantity: 1 };
export const orders = [
  { id: 'LD-240812', store: '강남 키친', menu: '토마토 파스타 × 1', pickup: '오늘 20:00', state: '픽업 대기', code: '6912', step: 2 },
  { id: 'LD-240810', store: '그린 키친', menu: '랜덤 샐러드 × 1', pickup: '8월 10일', state: '픽업 완료', step: 3 },
];
export const sellerOrders = [
  { id: 'LD-240812', menu: '토마토 파스타 × 1', pickup: '20:00', state: 'new', detail: '결제 완료 · 요청사항 없음' },
  { id: 'LD-240811', menu: '샌드위치 세트 × 1', pickup: '20:30', state: 'preparing', detail: '준비 시작 19:42 · 도착 시 연락 요청' },
  { id: 'LD-240810', menu: '랜덤박스 × 2', pickup: '20:00', state: 'waiting', detail: '고객 코드와 일치하는지 확인하세요.', code: '6912' },
  { id: 'LD-240809', menu: '샌드위치 세트 × 1', pickup: '19:10', state: 'done', detail: '픽업 완료 · 5,900원' },
];
