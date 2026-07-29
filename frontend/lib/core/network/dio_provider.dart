import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'api_base_url_provider.dart';
import 'compact_api_logging_interceptor.dart';
import 'token_storage_provider.dart';

part 'dio_provider.g.dart';

/// Gateway 진입점 하나만 호출한다 —
/// Member/Core 서비스 포트로 직접 호출 금지.
/// TODO: Android 에뮬레이터는 localhost가 아니라 10.0.2.2로 접근해야 함 —
/// 실기기/에뮬레이터에서 실제로 붙여볼 때 플랫폼별 baseUrl 분기 필요할 수 있음.

@Riverpod(keepAlive: true)
Dio dio(Ref ref) {
  final baseUrl = ref.watch(apiBaseUrlProvider);
  final dio = Dio(BaseOptions(baseUrl: baseUrl));

  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) async {
        // 로그인/회원가입/재발급은 토큰 없이 요청해야 하는 공개 엔드포인트다.
        // 이전 세션의 만료된 토큰이 저장소에 남아있으면 여기 붙어 나가서,
        // Gateway의 JWT 검증 필터가 permitAll 판단보다 먼저 401로 걸러버리고
        // 그 응답엔 CORS 헤더도 안 붙는다(troubleshooting 문서 참고) — 그래서
        // 이 경로들은 저장된 토큰 여부와 무관하게 헤더 자체를 안 붙인다.
        if (!_isPublicAuthPath(options.path)) {
          final tokenStorage = await ref.read(tokenStorageProvider.future);
          final accessToken = await tokenStorage.getAccessToken();
          if (accessToken != null) {
            options.headers['Authorization'] = 'Bearer $accessToken';
          }
        }
        handler.next(options);
      },
      // TODO: 401 응답 시 refresh 토큰으로 재시도하는 로직은 이번 주말 범위 밖.
      // 지금은 UnauthorizedException으로 정규화만 하고 재시도는 하지 않는다.
    ),
  );

  // 로컬 개발 중(디버그 빌드)에만 요청/응답을 콘솔에 찍는다 — 실제 요청 URL/바디,
  // 서버가 내려준 에러 바디(코드/메시지)까지 그대로 보여서, repository가 예외로
  // 정규화하기 전에 "서버가 실제로 뭘 줬는지"를 바로 확인할 수 있다. dio 기본
  // LogInterceptor는 요청당 여러 줄(헤더 포함)을 찍어서 콘솔이 금방 지저분해지길래,
  // 한 줄 요약 + 민감정보(비밀번호/토큰) 마스킹하는 자체 인터셉터를 대신 쓴다
  // (compact_api_logging_interceptor.dart). 릴리즈 빌드(`kDebugMode == false`)에서는
  // 아예 안 달아서 운영 로그에 안 남는다.
  if (kDebugMode) {
    dio.interceptors.add(CompactApiLoggingInterceptor());
  }

  return dio;
}

// Gateway의 permitAll 경로(GatewaySecurityConfig)와 동일하게 맞춘다.
const _publicAuthPaths = ['/auth/login', '/auth/signup', '/auth/refresh'];

bool _isPublicAuthPath(String path) =>
    _publicAuthPaths.any((publicPath) => path.endsWith(publicPath));
