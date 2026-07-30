import 'package:flutter/material.dart';

/// 앱 전역 색상 토큰. "티켓/입장권" 컨셉 — 마감할인 미스터리백을 픽업할 때
/// 실제로 픽업코드(티켓)를 보여주는 화면(B13)이 있어서, 그 정체성을 로그인부터
/// 전체 앱에 일관되게 심어둔다. 화면마다 색을 하드코딩하지 말고 여기서 가져다 쓴다.
///
/// 색 선택 근거:
/// - 브랜드 액센트는 "스탬프 레드"(도장 색) 하나만 — 버튼/CTA에서만 쓰고 카드보드와 안 섞는다.
/// - 배경은 순백 대신 따뜻한 페이퍼 톤 — 티켓 종이 재질감.
/// - success/warning/error는 브랜드 액센트와 분리된 기능색 (주문 상태 5종 구분용).
/// - 대비값은 실측 검증됨(WCAG 상대휘도 계산): 텍스트/배경 15.6:1, 카드보드/흰글씨 4.9~5.3:1.
abstract final class AppColors {
  // ── 브랜드 액센트 (스탬프 레드) ────────────────────────
  static const primary = Color(0xFFD6401F);
  static const primaryDark = Color(0xFFB7340F); // 뱃지 텍스트용(연한 배경 위)
  static const primaryLight = Color(0xFFFBE2DB); // 뱃지 배경용

  // ── 카드보드 (헤더 전용 — 골판지 줄무늬) ────────────────
  static const cardboard = Color(0xFF8C6B45);
  static const cardboardDark = Color(0xFF7F6039);

  // ── 페이퍼 (배경/표면/보더 — 따뜻한 티켓 종이 톤) ───────
  static const background = Color(0xFFF7F5F0);
  static const surface = Color(0xFFEFEBE2);
  static const border = Color(0xFFDCD5C6);

  // ── 텍스트 (잉크 색) ───────────────────────────────────
  static const textStrong = Color(0xFF22201C);
  static const textBody = Color(0xFF5C564A);
  static const textHint = Color(0xFF8A8378);
  static const textMeta = Color(0xFFB5AC9C); // 티켓 일련번호 같은 아주 옅은 메타 텍스트
  static const textOnPrimary = Colors.white;
  static const textOnCardboard = Colors.white;

  // ── 시맨틱 (기능색 — 주문 상태 6종 구분, 브랜드 액센트와 분리) ─
  static const error = Color(0xFFE64A4A);
  static const errorLight = Color(0xFFFCEAEA);
  static const success = Color(0xFF2E7D4F);
  static const successLight = Color(0xFFE8F6EC);
  static const warning = Color(0xFFE0972B);
  static const warningLight = Color(0xFFFCF1E1);
  static const waiting = Color(0xFF45607A); // 예약중(대기) 상태
  static const waitingLight = Color(0xFFE7EDF2);
  static const noShow = Color(0xFF8D5B4C); // 노쇼 — REJECTED(error)와 구분되는 별도 색
  static const noShowLight = Color(0xFFF3E7E3);
}
