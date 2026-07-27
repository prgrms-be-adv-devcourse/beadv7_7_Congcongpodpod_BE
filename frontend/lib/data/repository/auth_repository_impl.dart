import 'package:dio/dio.dart';

import '../../core/domain/error/app_exception.dart';
import '../../domain/error/auth_exception.dart';
import '../../domain/model/token_response.dart';
import '../../domain/repository/auth_repository.dart';
import '../../domain/repository/token_storage.dart';

/// AuthRepository 실제 구현. dio로 서버를 호출하고, 성공하면 토큰을 저장한다.
/// 예외를 잡아 우리 타입(AuthException/AppException)으로 정규화하는 것이 핵심 —
/// 화면은 dio의 DioException을 절대 보지 않는다.
class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl({required Dio dio, required TokenStorage tokenStorage})
    : _dio = dio,
      _tokenStorage = tokenStorage;

  final Dio _dio;
  final TokenStorage _tokenStorage;

  @override
  Future<void> login({required String email, required String password}) async {
    try {
      // dio의 baseUrl이 '.../api/v1' 이므로 여기선 뒷부분만.
      final response = await _dio.post(
        '/auth/login',
        data: {'email': email, 'password': password},
      );

      // 로그인 응답은 wrapper 없이 raw DTO.
      final tokens = TokenResponse.fromJson(
        response.data as Map<String, dynamic>,
      );

      await _tokenStorage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      // 400/401 → 자격 증명 문제. member-service가 에러코드를 안 주므로 상태코드로만 판단.
      if (status == 400 || status == 401) {
        throw const InvalidCredentialsException();
      }
      // 응답 자체가 없으면(연결 실패/타임아웃) 네트워크 문제.
      if (status == null) {
        throw const NetworkException();
      }
      throw ServerException('로그인 중 문제가 발생했습니다', code: status.toString());
    }
  }

  @override
  Future<void> signup({
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String password,
  }) async {
    try {
      await _dio.post(
        '/auth/signup',
        data: {
          'userName': userName,
          'password': password,
          'name': name,
          'phone': phone,
          'email': email,
          'role': 'MEMBER',
        },
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      // 400/401 → 가입 실패(중복 아이디 등). 로그인 실패와 원인이 다르므로 메시지 구분.
      if (status == 400 || status == 401) {
        throw const SignupFailedException();
      }
      // 응답 자체가 없으면(연결 실패/타임아웃) 네트워크 문제.
      if (status == null) {
        throw const NetworkException();
      }
      throw ServerException('회원가입 중 문제가 발생했습니다', code: status.toString());
    }

    // 회원가입 응답에 토큰이 없어서, 같은 자격증명으로 로그인을 한 번 더 호출해
    // 자동 로그인시킨다(의도된 동작 — 사용자 확인 완료).
    await login(email: email, password: password);
  }
}
