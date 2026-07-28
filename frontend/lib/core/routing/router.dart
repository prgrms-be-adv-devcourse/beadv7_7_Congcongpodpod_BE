import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod/riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../presentation/auth/login/login_screen.dart';
import '../../presentation/auth/signup/signup_screen.dart';
import '../../presentation/cart/cart_screen.dart';
import '../../presentation/deposit/charge/deposit_charge_fail_screen.dart';
import '../../presentation/deposit/charge/deposit_charge_screen.dart';
import '../../presentation/deposit/charge/deposit_charge_success_screen.dart';
import '../../presentation/deposit/deposit_screen.dart';
import '../../presentation/dev/screen_index_screen.dart';
import '../../presentation/member/mypage_screen.dart';
import '../../presentation/order/cancel/order_cancel_screen.dart';
import '../../presentation/order/checkout/checkout_screen.dart';
import '../../presentation/order/detail/order_detail_screen.dart';
import '../../presentation/order/list/order_list_screen.dart';
import '../../presentation/order/pickup/order_pickup_screen.dart';
import '../../presentation/seller/dish/seller_dish_screen.dart';
import '../../presentation/seller/order/seller_order_screen.dart';
import '../../presentation/seller/settlement/seller_settlement_screen.dart';
import '../../presentation/seller/store/seller_store_screen.dart';
import '../../presentation/seller/verify/seller_verify_screen.dart';
import '../../presentation/shell/main_shell_screen.dart';
import '../../presentation/store/detail/store_detail_screen.dart';
import '../../presentation/store/list/store_list_screen.dart';
import 'route_paths.dart';

part 'router.g.dart';

// 탭이 아닌 화면(장바구니, 체크아웃, 주문상세, 판매자 화면 등)을 하단 탭 위에
// "전체화면으로" 쌓기 위한 최상위 Navigator. StatefulShellRoute의 브랜치 안에
// 두면 하단 탭바가 계속 보여서(중첩 Navigator 안에서만 push된 것처럼 보임)
// 부자연스럽다 — go_router 공식 예제가 권장하는 parentNavigatorKey 패턴.
final _rootNavigatorKey = GlobalKey<NavigatorState>();

/// go_router 설정. 경로 문자열은 RoutePaths 상수에서 가져와 오타를 막는다.
/// 워킹 스켈레톤 — 전체 17개 화면(screens.md 기준) 네비게이션만 연결돼 있고
/// 신규 화면은 대부분 API/ViewModel 없는 뼈대(PlaceholderScreen)다.
@riverpod
GoRouter router(Ref ref) {
  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: RoutePaths.login,
    routes: [
      GoRoute(
        path: RoutePaths.login,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: RoutePaths.signup,
        builder: (context, state) => const SignupScreen(),
      ),
      GoRoute(
        path: RoutePaths.devScreenIndex,
        builder: (context, state) => const ScreenIndexScreen(),
      ),

      // 하단 탭 3개: 홈(B3) / 주문내역(B8) / 마이페이지(B12).
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            MainShellScreen(navigationShell: navigationShell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: RoutePaths.home,
                builder: (context, state) => const StoreListScreen(),
                routes: [
                  GoRoute(
                    path: 'stores/:storeId',
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (context, state) {
                      final storeId = int.parse(
                        state.pathParameters['storeId']!,
                      );
                      return StoreDetailScreen(storeId: storeId);
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: RoutePaths.orders,
                builder: (context, state) => const OrderListScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: RoutePaths.me,
                builder: (context, state) => const MyPageScreen(),
              ),
            ],
          ),
        ],
      ),

      // 탭 위에 쌓이는 화면들 — 전부 rootNavigatorKey로 전체화면 push.
      GoRoute(
        path: RoutePaths.cart,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const CartScreen(),
      ),
      GoRoute(
        path: RoutePaths.checkout,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const CheckoutScreen(),
      ),
      GoRoute(
        path: RoutePaths.orderDetail,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) =>
            OrderDetailScreen(orderId: state.pathParameters['orderId']!),
      ),
      GoRoute(
        path: RoutePaths.orderCancel,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) =>
            OrderCancelScreen(orderId: state.pathParameters['orderId']!),
      ),
      GoRoute(
        path: RoutePaths.orderPickup,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) =>
            OrderPickupScreen(orderId: state.pathParameters['orderId']!),
      ),
      GoRoute(
        path: RoutePaths.deposits,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const DepositScreen(),
      ),
      GoRoute(
        path: RoutePaths.depositCharge,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const DepositChargeScreen(),
      ),
      GoRoute(
        path: RoutePaths.depositChargeSuccess,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const DepositChargeSuccessScreen(),
      ),
      GoRoute(
        path: RoutePaths.depositChargeFail,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const DepositChargeFailScreen(),
      ),
      GoRoute(
        path: RoutePaths.sellerVerify,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const SellerVerifyScreen(),
      ),
      GoRoute(
        path: RoutePaths.sellerStore,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const SellerStoreScreen(),
      ),
      GoRoute(
        path: RoutePaths.sellerDishes,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const SellerDishScreen(),
      ),
      GoRoute(
        path: RoutePaths.sellerOrders,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const SellerOrderScreen(),
      ),
      GoRoute(
        path: RoutePaths.sellerSettlements,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (context, state) => const SellerSettlementScreen(),
      ),
    ],
  );
}
