# LastDish Frontend

Flutter 기반 LastDish 클라이언트입니다.

## 사전 준비

Flutter stable SDK와 Chrome이 필요합니다. Flutter SDK는 저장소에 포함하지
않으며 개발자별로 한 번만 설치합니다.

### macOS

```bash
brew install --cask flutter
flutter doctor
flutter config --enable-web
flutter devices
```

### Windows

1. [Git for Windows](https://git-scm.com/download/win)와 Chrome을 설치합니다.
2. [Flutter SDK archive](https://docs.flutter.dev/install/archive)에서 Windows용
   stable ZIP을 다운로드합니다.
3. ZIP을 `%USERPROFILE%\develop\flutter`에 압축 해제합니다. 권한 문제가 생길 수
   있으므로 `C:\Program Files` 아래에는 설치하지 않습니다.
4. Windows 사용자 환경 변수 `Path`에 다음 경로를 추가합니다.

```text
%USERPROFILE%\develop\flutter\bin
```

새 PowerShell을 열어 설치를 확인합니다.

```powershell
flutter doctor
flutter config --enable-web
flutter devices
```

설치 상세 내용은 [Flutter 공식 설치 문서](https://docs.flutter.dev/install)를
참고합니다.

## 웹 로컬 실행

세미프로젝트에서는 Flutter Web을 기본 대상으로 사용합니다. 저장소 루트에서
백엔드를 먼저 실행한 뒤 Chrome으로 프론트엔드를 실행합니다.

```bash
docker compose up -d --build
cd frontend
flutter pub get
flutter run -d chrome --web-port 3000
```

Windows PowerShell에서도 동일한 명령을 사용합니다.

```powershell
docker compose up -d --build
Set-Location frontend
flutter pub get
flutter run -d chrome --web-port 3000
```

Chrome이 자동으로 열리지 않으면 `http://localhost:3000`에 접속합니다.

기본 API 주소는 `http://localhost:8080/api/v1/`입니다. 다른 Gateway를 사용할
때는 실행 시 주소를 주입합니다.

```bash
flutter run -d chrome --web-port 3000 \
  --dart-define=API_BASE_URL=http://localhost:8080/api/v1/
```

Gateway의 `local` 프로필은 `http://localhost:3000`과
`http://127.0.0.1:3000`을 허용하므로 개발 서버 포트를 `3000`으로 유지합니다.

```bash
flutter run -d chrome --web-port 3000
```

## 웹 배포 빌드

```bash
flutter build web \
  --release \
  --dart-define=API_BASE_URL=https://api.example.com/api/v1/
```

정적 배포 파일은 `frontend/build/web/`에 생성됩니다. SPA 라우팅을 사용하는
호스팅 환경에서는 모든 경로가 `index.html`로 연결되도록 설정해야 합니다.

## 검증

```bash
flutter analyze
flutter test
```

## 종료

- Flutter Web: 실행 터미널에서 `q`
- 백엔드: 저장소 루트에서 `docker compose down`
