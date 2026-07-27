import 'package:freezed_annotation/freezed_annotation.dart';

part 'token_response.freezed.dart';
part 'token_response.g.dart';

/// 로그인/토큰재발급 응답. 백엔드 `TokenResponse{accessToken, refreshToken}`와 1:1 대응.
///
/// freezed가 불변 클래스 + copyWith + == 를, json_serializable이 fromJson 을 생성한다.
/// 이 파일을 고치면 `dart run build_runner build` 를 다시 돌려야 .freezed.dart / .g.dart 가 갱신된다.
@freezed
class TokenResponse with _$TokenResponse {
  const factory TokenResponse({
    required String accessToken,
    required String refreshToken,
  }) = _TokenResponse;

  factory TokenResponse.fromJson(Map<String, Object?> json) =>
      _$TokenResponseFromJson(json);
}
