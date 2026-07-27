import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../auth_repository_provider.dart';

part 'signup_view_model.g.dart';

/// 회원가입 화면의 상태(로딩/성공/에러)를 들고 있는 ViewModel.
///
/// build()가 void를 반환하므로 상태 타입은 `AsyncValue<void>` —
/// "데이터 값" 자체는 없고 로딩/성공/실패 3단계만 중요하기 때문.
/// login()이 성공하면 AsyncData(null), 실패하면 AsyncError가 되고,
/// 화면은 그 전이를 ref.listen으로 감지해 라우팅/스낵바를 처리한다.
@riverpod
class SignupViewModel extends _$SignupViewModel {
  @override
  FutureOr<void> build() {
    // 초기 상태 — 아직 아무 시도도 안 함.
  }

  Future<void> signup({
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String password,
  }) async {
    state = const AsyncLoading();
    // AsyncValue.guard: 안에서 던져진 예외를 자동으로 AsyncError로 감싼다 (try/catch 대체).
    state = await AsyncValue.guard(() async {
      final repository = await ref.read(authRepositoryProvider.future);
      await repository.signup(
        userName: userName,
        password: password,
        name: name,
        phone: phone,
        email: email,
      );
    });
  }
}
