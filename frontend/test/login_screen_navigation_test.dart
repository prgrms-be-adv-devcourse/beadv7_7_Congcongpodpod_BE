import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:lastdish_app/core/routing/route_paths.dart';
import 'package:lastdish_app/domain/error/auth_exception.dart';
import 'package:lastdish_app/domain/repository/auth_repository.dart';
import 'package:lastdish_app/presentation/auth/auth_repository_provider.dart';
import 'package:lastdish_app/presentation/auth/login/login_screen.dart';

/// 로그인 실패를 재현하기 위한 가짜 리포지토리 — 항상 InvalidCredentialsException을 던진다.
class _FailingAuthRepository implements AuthRepository {
  @override
  Future<void> login({required String email, required String password}) {
    throw const InvalidCredentialsException();
  }

  @override
  Future<void> signup({
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String password,
  }) {
    throw UnimplementedError();
  }
}

/// 로그인 성공을 재현하기 위한 가짜 리포지토리 — 아무 것도 안 하고 그냥 끝낸다.
class _SucceedingAuthRepository implements AuthRepository {
  @override
  Future<void> login({required String email, required String password}) async {}

  @override
  Future<void> signup({
    required String userName,
    required String name,
    required String phone,
    required String email,
    required String password,
  }) {
    throw UnimplementedError();
  }
}

GoRouter _buildTestRouter() {
  return GoRouter(
    initialLocation: RoutePaths.login,
    routes: [
      GoRoute(
        path: RoutePaths.login,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: RoutePaths.home,
        builder: (context, state) => const Text('HOME_SENTINEL'),
      ),
    ],
  );
}

void main() {
  testWidgets('로그인 실패 시 홈으로 넘어가지 않고 에러만 보여준다', (tester) async {
    final router = _buildTestRouter();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authRepositoryProvider.overrideWith(
            (ref) async => _FailingAuthRepository(),
          ),
        ],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).at(0), 'a@b.com');
    await tester.enterText(find.byType(TextField).at(1), 'wrongpassword');
    await tester.tap(find.text('입장하기'));
    // 로딩 -> 에러까지 전이가 다 끝날 때까지 펌프.
    await tester.pumpAndSettle();

    expect(
      find.text('HOME_SENTINEL'),
      findsNothing,
      reason: '로그인 실패인데 홈으로 이동했다',
    );
    expect(find.text('입장하기'), findsOneWidget, reason: '로그인 화면에 그대로 남아있어야 한다');
  });

  testWidgets('로그인 성공 시 홈으로 넘어간다', (tester) async {
    final router = _buildTestRouter();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authRepositoryProvider.overrideWith(
            (ref) async => _SucceedingAuthRepository(),
          ),
        ],
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).at(0), 'a@b.com');
    await tester.enterText(find.byType(TextField).at(1), 'correctpassword');
    await tester.tap(find.text('입장하기'));
    await tester.pumpAndSettle();

    expect(find.text('HOME_SENTINEL'), findsOneWidget, reason: '로그인 성공인데 홈으로 안 넘어갔다');
  });
}
