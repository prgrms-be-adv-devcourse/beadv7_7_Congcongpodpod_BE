# Frontend

- `flutter/`: 기존 Flutter 구현 보존
- `react-native/`: 신규 Expo + React Native 구현

React Native 실행:

```bash
cd frontend/react-native
cp .env.example .env.local
# .env.local의 NAVER_MAP_CLIENT_ID 입력
npm run ios
```

네이버 지도는 Expo Go가 아닌 development build가 필요합니다.
네이버 클라우드 Maps 애플리케이션에는 Android 패키지
`com.congcongpodpod.lastdish_app`과 iOS 번들 ID
`com.congcongpodpod.lastdishApp`을 등록해야 합니다.
