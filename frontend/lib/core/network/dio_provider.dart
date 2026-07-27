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
        final tokenStorage = await ref.read(tokenStorageProvider.future);
        final accessToken = await tokenStorage.getAccessToken();
        if (accessToken != null) {
          options.headers['Authorization'] = 'Bearer $accessToken';
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
