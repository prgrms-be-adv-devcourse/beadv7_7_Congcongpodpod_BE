import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lastdish_app/presentation/auth/signup/signup_screen.dart';

void main() {
  Future<void> pumpSignup(WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(child: MaterialApp(home: SignupScreen())),
    );
  }

  testWidgets('전화번호 필드에 숫자 11자리 입력 시 010-1234-5678 형태로 하이픈 자동 삽입', (
    tester,
  ) async {
    await pumpSignup(tester);

    // 필드 순서: 아이디(0), 이름(1), 핸드폰 번호(2), 이메일(3).
    final phoneField = find.byType(TextField).at(2);
    await tester.enterText(phoneField, '01012345678');
    await tester.pump();

    final field = tester.widget<TextField>(phoneField);
    expect(field.controller!.text, '010-1234-5678');
  });

  testWidgets('비밀번호 8자 미만이면 제출 시 검증 에러 노출', (tester) async {
    await pumpSignup(tester);

    await tester.enterText(find.byType(TextFormField), 'short');
    await tester.tap(find.text('가입하기'));
    await tester.pump();

    expect(find.text('비밀번호는 8자 이상이어야 합니다'), findsOneWidget);
  });
}
