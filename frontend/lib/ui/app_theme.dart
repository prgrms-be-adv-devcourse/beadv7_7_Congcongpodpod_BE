import 'package:flutter/material.dart';

import 'app_colors.dart';
import 'app_radius.dart';
import 'app_spacing.dart';
import 'app_typography.dart';

/// MaterialApp에 넘길 전역 테마. "티켓" 컨셉 — 버튼은 각지게(AppRadius.sharp),
/// 인풋은 밑줄형(종이 티켓에 손으로 적는 느낌)으로 통일.
/// 각 화면은 이 테마를 상속하므로 위젯마다 색·모양을 반복 지정하지 않아도 된다.
abstract final class AppTheme {
  static ThemeData get light => ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: AppColors.surface,
    textTheme: AppTypography.textTheme,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: Brightness.light,
      primary: AppColors.primary,
      onPrimary: AppColors.textOnPrimary,
      error: AppColors.error,
      surface: AppColors.background,
      surfaceContainerHighest: AppColors.surface,
      outline: AppColors.border,
    ),
    // 기본 버튼: 스탬프 레드, 각진 모서리(티켓 특유의 느낌 — 둥근 카드와 대비)
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.textOnPrimary,
        minimumSize: const Size.fromHeight(48),
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.sharp),
        ),
        textStyle: AppTypography.textTheme.labelLarge?.copyWith(
          color: AppColors.textOnPrimary,
          fontWeight: FontWeight.w800,
        ),
      ),
    ),
    // OutlinedButton은 색만 다를 뿐 ElevatedButton과 같은 "각진 버튼" 계열이라
    // 모서리(shape)는 맞춘다 — 크기(minimumSize)는 안 건드린다. 화면마다 작게
    // 쓰는 outlined 버튼(예: 예치금 화면의 "충전하기")이 있어서, 크기까지 강제하면
    // 그 화면들 레이아웃이 깨진다.
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.sharp),
        ),
      ),
    ),
    // 인풋: 밑줄형 — 박스/필 없이 라벨은 위에 별도로 두고 인풋 자체는 최소한으로.
    inputDecorationTheme: InputDecorationTheme(
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      border: const UnderlineInputBorder(
        borderSide: BorderSide(color: AppColors.border),
      ),
      enabledBorder: const UnderlineInputBorder(
        borderSide: BorderSide(color: AppColors.border),
      ),
      focusedBorder: const UnderlineInputBorder(
        borderSide: BorderSide(color: AppColors.primary, width: 1.5),
      ),
      errorBorder: const UnderlineInputBorder(
        borderSide: BorderSide(color: AppColors.error),
      ),
      hintStyle: AppTypography.textTheme.bodyMedium?.copyWith(
        color: AppColors.textHint,
      ),
    ),
    cardTheme: CardThemeData(
      color: AppColors.background,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.md),
        side: const BorderSide(color: AppColors.border),
      ),
    ),
    dividerTheme: const DividerThemeData(
      color: AppColors.border,
      thickness: 1,
      space: 1,
    ),
  );
}
