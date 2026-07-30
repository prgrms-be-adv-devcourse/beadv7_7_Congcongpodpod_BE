import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/browser_url.dart';
import '../../../core/routing/route_paths.dart';
import '../../../ui/app_spacing.dart';

/// Toss 결제창이 실패/취소 시 리다이렉트하는 화면 (`/deposits/charge/fail`).
/// Toss가 `code`/`message` 쿼리파라미터로 실패 사유를 실어준다 — 성공 화면과 같은
/// 이유로 `Uri.base.queryParameters`에서 직접 읽는다(go_router 쿼리 아님).
class DepositChargeFailScreen extends StatefulWidget {
  const DepositChargeFailScreen({super.key});

  @override
  State<DepositChargeFailScreen> createState() =>
      _DepositChargeFailScreenState();
}

class _DepositChargeFailScreenState extends State<DepositChargeFailScreen> {
  late final String _message;

  @override
  void initState() {
    super.initState();
    // build()가 도는 시점엔 이미 지워져 있을 수 있으니, 지우기 전에 먼저 읽어둔다.
    _message = Uri.base.queryParameters['message'] ?? '결제가 취소되었거나 실패했어요.';
    stripLeakedQueryFromUrl();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('결제 실패')),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: AppSpacing.md),
              Text(_message, textAlign: TextAlign.center),
              const SizedBox(height: AppSpacing.lg),
              ElevatedButton(
                onPressed: () {
                  // 성공 화면과 같은 이유(결제 리다이렉트로 스택이 이 화면
                  // 하나뿐임) — 홈으로 스택을 새로 잡고 그 위에 push한다.
                  // go() 직후 바로 push()하면 go()의 라우터 상태 반영이 아직
                  // 안 끝난 시점이라 push가 옛 스택(이 화면) 위에 얹혀서
                  // 뒤로가기가 이 화면으로 돌아오는 버그가 있었다(2026-07-30
                  // 발견) — 다음 프레임으로 미뤄서 go()가 완전히 반영된 뒤에
                  // push하도록 고쳤다.
                  context.go(RoutePaths.home);
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (context.mounted) {
                      context.push(RoutePaths.depositCharge);
                    }
                  });
                },
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
