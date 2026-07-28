// 로그인 화면이 정상적으로 뜨는지 확인하는 스모크 테스트.
// LastDishApp은 ref(Riverpod)를 쓰므로 ProviderScope로 감싸야 한다 —
// main.dart의 runApp(ProviderScope(child: LastDishApp())) 구조를 테스트에서도 그대로 따라간다.

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lastdish_app/main.dart';

void main() {
  testWidgets('로그인 화면이 뜬다', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: LastDishApp()),
    );
    await tester.pumpAndSettle();

    expect(find.text('LastDish'), findsOneWidget);
    expect(find.text('입장하기'), findsOneWidget);
  });
}
