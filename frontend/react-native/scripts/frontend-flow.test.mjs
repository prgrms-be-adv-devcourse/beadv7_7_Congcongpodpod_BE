import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import test from 'node:test';

const root = new URL('..', import.meta.url).pathname;
const source = (path) => readFileSync(join(root, path), 'utf8');

test('구매 핵심 화면이 모두 존재한다', () => {
  ['src/app/login.tsx', 'src/app/(tabs)/stores.tsx', 'src/app/stores/[storeId].tsx', 'src/app/dishes/[dishId].tsx', 'src/app/cart.tsx', 'src/app/cart/checkout.tsx', 'src/app/(tabs)/orders.tsx']
    .forEach((path) => assert.equal(existsSync(join(root, path)), true, path));
});

test('지도 앱·웹이 동일한 화면 경계를 전달한다', () => {
  const nativeMap = source('src/components/map-canvas.native.tsx');
  assert.match(nativeMap, /southWest: \{ latitude: region\.latitude, longitude: region\.longitude \}/);
  assert.match(nativeMap, /northEast: \{ latitude: region\.latitude \+ region\.latitudeDelta, longitude: region\.longitude \+ region\.longitudeDelta \}/);
  assert.match(source('src/components/map-canvas.web.tsx'), /getBounds\(\)[\s\S]*southWest[\s\S]*northEast/);
  assert.match(source('src/app/(tabs)/index.tsx'), /reload\(next, false, next\.bounds\)/);
  assert.match(source('src/lib/stores.ts'), /page < totalPages/);
});

test('로그인 제한 탭은 로그인 화면으로 이동한다', () => {
  const tabs = source('src/app/(tabs)/_layout.tsx');
  assert.match(tabs, /router\.push\(\{ pathname: '\/login'/);
  assert.match(tabs, /name="favorites" listeners=\{requireLogin/);
  assert.match(tabs, /name="orders" listeners=\{requireLogin/);
});

test('찜과 주문 변경은 공용 서버 캐시를 무효화한다', () => {
  assert.match(source('src/lib/favorites.ts'), /invalidateQueries\('favorites'\)/);
  assert.match(source('src/lib/orders.ts'), /invalidateQueries\('orders:'\)/);
});

test('매장·상품 이미지는 디스크 캐시와 실패 대체 이미지를 사용한다', () => {
  const image = source('src/components/optimized-image.tsx');
  assert.match(image, /cachePolicy="memory-disk"/);
  assert.match(image, /setFailed\(true\)/);
});

test('라우트 오류 경계와 요청 취소가 구성되어 있다', () => {
  assert.match(source('src/app/_layout.tsx'), /export function ErrorBoundary/);
  assert.match(source('src/lib/api.ts'), /RequestCancelledError/);
  assert.match(source('src/hooks/use-nearby-stores.ts'), /requestControllerRef\.current\?\.abort\(\)/);
});

test('SSE는 토큰 갱신·heartbeat·지수 백오프를 사용한다', () => {
  const provider = source('src/providers/notification-provider.tsx');
  const stream = source('src/lib/notifications.ts');
  assert.match(provider, /2 \*\* Math\.max\(0, attempt\)/);
  assert.match(provider, /refreshAccessToken\(\)/);
  assert.match(provider, /EXPO_PUBLIC_NOTIFICATION_DEMO === 'true'/);
  assert.match(stream, /HEARTBEAT_TIMEOUT_MS/);
  assert.match(stream, /onConnected\?\.\(\)/);
});

test('주문 불가 장바구니 상품은 유지하고 결제를 차단한다', () => {
  const cart = source('src/app/cart.tsx');
  const checkout = source('src/app/cart/checkout.tsx');
  const cartApi = source('src/lib/cart.ts');
  assert.match(cartApi, /INSUFFICIENT_STOCK/);
  assert.match(cartApi, /OUT_OF_STOCK/);
  assert.match(cartApi, /DISH_UNAVAILABLE/);
  assert.match(cart, /disabled=\{!availability\?\.orderable\}/);
  assert.match(checkout, /\['ORD007','D001','D003'\]/);
  assert.match(checkout, /router\.replace\('\/cart'\)/);
});
