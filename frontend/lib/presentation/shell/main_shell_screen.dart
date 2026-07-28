import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// 하단 탭(홈/주문내역/마이페이지) 셸 (screens.md §5 3탭 가정 반영).
/// `StatefulShellRoute.indexedStack`이 브랜치별 화면 상태(스크롤 위치 등)를
/// `IndexedStack`으로 유지한 채 탭을 전환해준다 — 탭 전환마다 새로 build하면
/// 잃어버리는 상태를 지키기 위한 go_router 표준 패턴이라 그대로 채택.
class MainShellScreen extends StatelessWidget {
  const MainShellScreen({required this.navigationShell, super.key});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: navigationShell.goBranch,
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), label: '홈'),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            label: '주문내역',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: '마이페이지',
          ),
        ],
      ),
    );
  }
}
