import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'api_base_url_provider.g.dart';

// 배포 서버(gateway) 직결. 로컬 8080/에뮬레이터 10.0.2.2 분기는 실제 인터넷
// 호스트라 필요 없어져서 제거 — 로컬 재기동 필요해지면 그때 다시 분기.
// Vercel 등 빌드 환경에서는 --dart-define=API_BASE_URL=https://api.lastdish.kr/api/v1
// 로 덮어씀. 값 없으면(로컬 flutter run) 기존 localhost 게이트웨이로 폴백.
const _apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080/api/v1/',
);

@Riverpod(keepAlive: true)
String apiBaseUrl(Ref ref) => _apiBaseUrl;
