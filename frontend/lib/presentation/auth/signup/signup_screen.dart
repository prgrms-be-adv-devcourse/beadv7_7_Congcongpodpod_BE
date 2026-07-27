import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/route_paths.dart';
import 'signup_view_model.dart';

/// 숫자 입력을 010-1234-5678 형태(3-4-4)로 자동 하이픈 삽입.
/// 국번 다른 전화(02 등)는 스코프 밖 — 회원가입은 휴대폰 번호 기준.
class _PhoneNumberFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    final limited = digits.length > 11 ? digits.substring(0, 11) : digits;

    final buffer = StringBuffer();
    for (var i = 0; i < limited.length; i++) {
      buffer.write(limited[i]);
      if ((i == 2 || i == 6) && i != limited.length - 1) {
        buffer.write('-');
      }
    }

    final formatted = buffer.toString();
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class SignupScreen extends ConsumerStatefulWidget {
  const SignupScreen({super.key});

  @override
  ConsumerState<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends ConsumerState<SignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _userNameController = TextEditingController();
  final _nameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _userNameController.dispose();
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    ref
        .read(signupViewModelProvider.notifier)
        .signup(
          userName: _userNameController.text.trim(),
          name: _nameController.text.trim(),
          phone: _phoneController.text.trim(),
          email: _emailController.text.trim(),
          password: _passwordController.text,
        );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(signupViewModelProvider);
    final isLoading = state.isLoading;

    ref.listen(signupViewModelProvider, (previous, next) {
      final wasLoading = previous?.isLoading ?? false;
      if (!wasLoading || next.isLoading) return; // 로딩이 끝난 전이가 아니면 무시.

      // ⚠️ next.hasValue로 성공 여부를 판단하면 안 된다(login_screen.dart와 동일한 이유) —
      // AsyncNotifier가 로딩/에러 전이에서도 초기 상태(build()의 hasValue=true)를
      // copyWithPrevious로 계속 이어받아서, 회원가입이 실패해도 next.hasValue가 true로
      // 남는다. hasError를 먼저 확인해서 갈라야 한다.
      if (next.hasError) {
        // login_screen.dart와 같은 이유 — 디버그 빌드에서만 콘솔에 전체 스택트레이스를 남긴다.
        if (kDebugMode) {
          debugPrint('[signup] ${next.error}\n${next.stackTrace}');
        }
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(next.error.toString())));
      } else {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('가입 완료!')));
        context.go(RoutePaths.home); // 자동 로그인이므로 홈으로
      }
    });

    return Scaffold(
      appBar: AppBar(title: const Text('회원가입')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('아이디', style: Theme.of(context).textTheme.labelSmall),
                TextField(
                  controller: _userNameController,
                  enabled: !isLoading,
                  decoration: const InputDecoration(hintText: '아이디 입력'),
                ),
                const SizedBox(height: 16),
                Text('이름', style: Theme.of(context).textTheme.labelSmall),
                TextField(
                  controller: _nameController,
                  enabled: !isLoading,
                  decoration: const InputDecoration(hintText: '이름 입력'),
                ),
                const SizedBox(height: 16),
                Text('핸드폰 번호', style: Theme.of(context).textTheme.labelSmall),
                TextField(
                  controller: _phoneController,
                  enabled: !isLoading,
                  keyboardType: TextInputType.phone,
                  inputFormatters: [_PhoneNumberFormatter()],
                  decoration: const InputDecoration(hintText: '010-1234-5678'),
                ),
                const SizedBox(height: 16),
                Text('이메일', style: Theme.of(context).textTheme.labelSmall),
                TextField(
                  controller: _emailController,
                  enabled: !isLoading,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(hintText: '이메일 입력'),
                ),
                const SizedBox(height: 16),
                Text('비밀번호', style: Theme.of(context).textTheme.labelSmall),
                TextFormField(
                  controller: _passwordController,
                  enabled: !isLoading,
                  obscureText: true,
                  onFieldSubmitted: (_) => isLoading ? null : _submit(),
                  decoration: const InputDecoration(
                    hintText: '비밀번호 입력 (8자 이상)',
                  ),
                  validator: (value) => (value == null || value.length < 8)
                      ? '비밀번호는 8자 이상이어야 합니다'
                      : null,
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: isLoading ? null : _submit,
                  child: isLoading
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('가입하기'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
