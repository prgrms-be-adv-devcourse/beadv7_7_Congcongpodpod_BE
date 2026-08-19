# LastDish universal frontend

Expo Router 기반 iOS·Android·Web 공용 프론트엔드입니다. 화면·API·상태 로직은 공유하고 지도, 결제, 인증 저장소처럼 플랫폼 의존적인 기능만 `.native.tsx`와 `.web.tsx`로 분리합니다.

## 실행

```bash
npm install
npm run ios
npm run android
npm run web
```

정적 웹 빌드:

```bash
npm run web:build
```

결과물은 `dist/`에 생성됩니다.

## 환경 변수

`.env.example`을 기준으로 `.env.local`을 작성합니다.

- `EXPO_PUBLIC_API_BASE_URL`: 백엔드 API 주소
- `NAVER_MAP_CLIENT_ID`: iOS·Android 네이버 지도 Client ID
- `EXPO_PUBLIC_NAVER_MAP_CLIENT_ID`: 웹 네이버 지도 Client ID
- `EXPO_PUBLIC_KAKAO_REST_API_KEY`: 카카오 REST API 키
- `EXPO_PUBLIC_KAKAO_REDIRECT_URI`: 네이티브 카카오 Redirect URI
- `EXPO_PUBLIC_KAKAO_WEB_REDIRECT_URI`: 웹 카카오 Redirect URI

네이버 Cloud Maps 애플리케이션의 Web 서비스 URL에 운영 도메인과 로컬 개발 주소를 등록해야 합니다. 카카오 개발자 콘솔에도 `EXPO_PUBLIC_KAKAO_WEB_REDIRECT_URI`와 동일한 Redirect URI를 등록해야 합니다.

## 플랫폼 분리

- `src/components/map-canvas.native.tsx`: 네이티브 네이버 지도
- `src/components/map-canvas.web.tsx`: 네이버 지도 JavaScript API
- `src/components/deposit-payment.native.tsx`: Toss React Native 결제위젯
- `src/components/deposit-payment.web.tsx`: Toss JavaScript 결제위젯
- `src/lib/storage.native.ts`: SecureStore
- `src/lib/storage.web.ts`: 브라우저 저장소
