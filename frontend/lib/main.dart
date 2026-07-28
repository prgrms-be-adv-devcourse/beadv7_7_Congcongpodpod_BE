import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/presentation/components/mobile_width_constraint.dart';
import 'core/routing/router.dart';
import 'ui/app_theme.dart';

void main() {
  // ProviderScope: Riverpod의 모든 Provider가 사는 최상위 컨테이너.
  // 앱 전체를 감싸야 어디서든 ref로 Provider를 읽을 수 있다.
  runApp(const ProviderScope(child: LastDishApp()));
}

class LastDishApp extends ConsumerWidget {
  const LastDishApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    // MaterialApp.router: go_router를 쓰는 경우의 진입점.
    return MaterialApp.router(
      title: 'LastDish',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      routerConfig: router,
      // 웹/데스크톱에서 화면 전체를 안 채우고 모바일 폭으로 가운데 고정.
      // 여기 한 곳만 감싸면 모든 화면(go_router가 만드는 Navigator)에 자동 적용.
      builder: (context, child) => MobileWidthConstraint(child: child!),
    );
  }
}
