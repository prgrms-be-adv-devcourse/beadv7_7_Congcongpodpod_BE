import 'dart:convert';

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

      // 2026-07-27 실제 로그로 확인: 로그인 응답도 core-service처럼
      // {success,data,timestamp} 래퍼가 있다(예전엔 "래퍼 없음"으로 잘못 가정하고 있었음
      // — TokenResponse.fromJson에 래퍼 전체를 넘겨서 accessToken/refreshToken이 둘 다
      // null로 읽혀 TypeError가 났었다). 실제 토큰은 `data` 안에 있다.
      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      final tokens = TokenResponse.fromJson(data);

      await _tokenStorage.saveTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      // 응답 자체가 없으면(연결 실패/타임아웃) 네트워크 문제.
      if (status == null) {
        throw const NetworkException();
      }
      // 4xx 전부 클라이언트 쪽 문제(자격 증명 등)로 본다 — 정확히 400/401만 체크했더니
      // 실제로는 409(Conflict) 등 다른 4xx도 내려온다는 게 확인됨(2026-07-27,
      // signup에서 아이디 중복이 409로 옴). 서버가 {error:{code,message}} 형태로
      // 이유를 내려주므로, 있으면 그 메시지를 그대로 쓰고 없으면 기본 문구로 대체.
      if (status >= 400 && status < 500) {
        throw InvalidCredentialsException(
          _serverMessage(e) ?? const InvalidCredentialsException().message,
        );
      }
      throw ServerException('로그인 중 문제가 발생했습니다', code: status.toString());
    }
  }

  /// 에러 바디에서 message를 뽑는다. member-service가 실패 원인마다 응답 모양이
  /// 다를 수 있어서(2026-07-27 확인 — 아직 완전히 통일 안 됨, api-contracts.md 0절
  /// 참고) 세 가지 형태를 순서대로 시도한다:
  /// 1. core-service와 같은 래퍼 — `{success:false, error:{code, message}}`
  /// 2. Spring Boot 기본 에러 바디(BusinessException으로 안 잡힌 경우) — `{message: "..."}`가 최상위
  /// 3. body가 아예 Map이 아니라 JSON 문자열로 온 경우 — 디코드해서 위 둘을 다시 시도
  /// 셋 다 아니면 null — 호출부가 기본 문구로 대체한다.
  String? _serverMessage(DioException e) {
    var body = e.response?.data;
    if (body is String) {
      try {
        body = jsonDecode(body);
      } on FormatException {
        return null;
      }
    }
    if (body is Map<String, dynamic>) {
      final error = body['error'];
      if (error is Map<String, dynamic>) {
        final message = error['message'] as String?;
        if (message != null) return message;
      }
      final topLevelMessage = body['message'];
      if (topLevelMessage is String) return topLevelMessage;
    }
    return null;
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
        },
      );
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      // 응답 자체가 없으면(연결 실패/타임아웃) 네트워크 문제.
      if (status == null) {
        throw const NetworkException();
      }
      // 4xx 전부 가입 실패(중복 아이디 등)로 본다 — 400/401만 체크했더니 실제로는
      // 409(Conflict)로 옴이 확인됨(2026-07-27, `M006 이미 사용 중인 아이디입니다`).
      // 서버 메시지가 있으면 그대로 보여준다 — 로그인 실패와는 기본 문구로만 구분.
      if (status >= 400 && status < 500) {
        throw SignupFailedException(
          _serverMessage(e) ?? const SignupFailedException().message,
        );
      }
      throw ServerException('회원가입 중 문제가 발생했습니다', code: status.toString());
    }

    // 회원가입 응답에 토큰이 없어서, 같은 자격증명으로 로그인을 한 번 더 호출해
    // 자동 로그인시킨다(의도된 동작 — 사용자 확인 완료).
    await login(email: email, password: password);
  }
}
