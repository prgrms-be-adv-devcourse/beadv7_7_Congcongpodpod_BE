import 'package:dio/dio.dart';
import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'api_base_url_provider.dart';
import 'token_storage_provider.dart';

part 'dio_provider.g.dart';

/// Gateway 진입점 하나만 호출한다 —
/// Member/Core 서비스 포트로 직접 호출 금지.
/// 환경별 주소는 `--dart-define=API_BASE_URL=...`로 주입한다.

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

  return dio;
}
