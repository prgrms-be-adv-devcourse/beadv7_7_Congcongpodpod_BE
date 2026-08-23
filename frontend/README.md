# Frontend

`react-native/`에서 Expo 기반 iOS·Android·Web 클라이언트를 공용 코드로 구현합니다. 플랫폼 API가 다른 지도·결제·저장소만 동일 계약의 네이티브·웹 어댑터로 분리합니다.

React Native 실행:

```bash
cd frontend/react-native
cp .env.example .env.local
# .env.local의 NAVER_MAP_CLIENT_ID 입력
npm run ios
```

웹 실행은 `npm run web`, Android 실행은 `npm run android`를 사용합니다. 전체 환경변수와 플랫폼별 요구사항은 [`react-native/README.md`](react-native/README.md)를 확인하세요.

네이버 지도는 Expo Go가 아닌 development build가 필요합니다.
네이버 클라우드 Maps 애플리케이션에는 Android 패키지
`com.congcongpodpod.lastdish_app`과 iOS 번들 ID
`com.congcongpodpod.lastdishApp`을 등록해야 합니다.
