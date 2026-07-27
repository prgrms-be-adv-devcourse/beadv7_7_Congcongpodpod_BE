import 'package:flutter/material.dart';

import 'app_colors.dart';

/// 타이포그래피 스케일. 화면에서 `TextStyle(fontSize: 20, fontWeight: ...)`를
/// 직접 쓰지 말고 `Theme.of(context).textTheme.titleLarge` 처럼 역할로 가져다 쓴다.
///
/// 역할 가이드:
/// - display : 거의 안 씀(온보딩 등 아주 큰 히어로 텍스트)
/// - headline: 화면 상단 큰 타이틀 (로고, "LastDish" 같은 것)
/// - title   : 섹션 제목, AppBar 제목, 카드 제목
/// - body    : 본문 텍스트 (기본값)
/// - label   : 버튼 텍스트, 뱃지, 캡션류 작은 텍스트
// 커스텀 폰트 없음 — 시스템 기본(플랫폼별 San Francisco/Roboto) 그대로 사용.
abstract final class AppTypography {
  static TextTheme get textTheme => const TextTheme(
    displayLarge: TextStyle(
      fontSize: 32,
      fontWeight: FontWeight.w800,
      color: AppColors.textStrong,
      height: 1.2,
    ),
    headlineLarge: TextStyle(
      fontSize: 28,
      fontWeight: FontWeight.w700,
      color: AppColors.textStrong,
      height: 1.25,
    ),
    headlineMedium: TextStyle(
      fontSize: 22,
      fontWeight: FontWeight.w700,
      color: AppColors.textStrong,
      height: 1.3,
    ),
    headlineSmall: TextStyle(
      fontSize: 20,
      fontWeight: FontWeight.w600,
      color: AppColors.textStrong,
      height: 1.3,
    ),
    titleLarge: TextStyle(
      fontSize: 18,
      fontWeight: FontWeight.w600,
      color: AppColors.textStrong,
      height: 1.4,
    ),
    titleMedium: TextStyle(
      fontSize: 16,
      fontWeight: FontWeight.w600,
      color: AppColors.textStrong,
      height: 1.4,
    ),
    titleSmall: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w600,
      color: AppColors.textBody,
      height: 1.4,
    ),
    bodyLarge: TextStyle(
      fontSize: 16,
      fontWeight: FontWeight.w400,
      color: AppColors.textStrong,
      height: 1.5,
    ),
    bodyMedium: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w400,
      color: AppColors.textBody,
      height: 1.5,
    ),
    bodySmall: TextStyle(
      fontSize: 12,
      fontWeight: FontWeight.w400,
      color: AppColors.textHint,
      height: 1.5,
    ),
    labelLarge: TextStyle(
      fontSize: 15,
      fontWeight: FontWeight.w600,
      color: AppColors.textStrong,
      height: 1.2,
    ),
    labelMedium: TextStyle(
      fontSize: 12,
      fontWeight: FontWeight.w600,
      color: AppColors.textBody,
      height: 1.2,
    ),
    labelSmall: TextStyle(
      fontSize: 11,
      fontWeight: FontWeight.w500,
      color: AppColors.textHint,
      height: 1.2,
    ),
  );
}
