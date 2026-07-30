import 'package:flutter/services.dart';

/// 전화번호 입력창에서 타이핑하는 대로 자동으로 하이픈을 붙인다.
///
/// phone_format.dart의 formatPhone(최종 자리수 9/10/11 기준 분기)을 타이핑
/// 도중에도 그대로 쓰면 안 된다 — 예를 들어 9자리째는 무조건 서울 국번(2-3-4)으로
/// 취급해서, 010으로 시작하는 휴대폰 번호(최종 11자리, 3-4-4)를 치는 중간에
/// "01-047-…" 처럼 순간적으로 잘못 잘린다(2026-07-30 발견). 대신 여기서는
/// "끝 4자리는 항상 마지막 그룹, 국번 뒤 나머지는 가운데 그룹"으로 매 입력마다
/// 다시 계산한다 — 최종 자리수를 몰라도 안정적으로 자라나고, 마지막에 항상
/// 올바른 3-4-4 / 2-4-4 / 3-3-4 / 2-3-4 형태로 맞아떨어진다.
///
/// ⚠️ 총 자리수 상한을 국번 종류와 무관하게 11로만 걸면(2026-07-30 발견),
/// 서울(02, 최대 10자리)인데 11자리까지 받아버려서 가운데 그룹이 5자리
/// ("02-12345-6799")까지 늘어나는 잘못된 상태가 만들어진다 — 검증 정규식(2~3자리
/// 국번 + 가운데 3~4자리)과 애초에 안 맞는 모양이라 사용자가 지우는 것 말고는
/// 빠져나올 방법이 없었다. 그래서 국번이 "02"면 상한을 10으로, 그 외엔 11로
/// 둬서 애초에 그 상태에 들어갈 수 없게 막는다.
class PhoneNumberInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    final maxDigits = digits.startsWith('02') ? 10 : 11;
    final limited = digits.length > maxDigits ? digits.substring(0, maxDigits) : digits;
    final formatted = _group(limited);

    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }

  String _group(String digits) {
    // 서울(02)만 국번이 2자리, 나머지(010/011 등 휴대폰, 031~064 등 지역번호)는 3자리.
    final areaLength = digits.startsWith('02') ? 2 : 3;
    if (digits.length <= areaLength) return digits;

    final area = digits.substring(0, areaLength);
    final rest = digits.substring(areaLength);
    if (rest.length <= 4) return '$area-$rest';

    final lastGroup = rest.substring(rest.length - 4);
    final middleGroup = rest.substring(0, rest.length - 4);
    return '$area-$middleGroup-$lastGroup';
  }
}

/// 사업자등록번호 입력창에서 000-00-00000(3-2-5) 형태로 자동 하이픈 삽입.
class BusinessNumberInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    final limited = digits.length > 10 ? digits.substring(0, 10) : digits;

    final buffer = StringBuffer();
    for (var i = 0; i < limited.length; i++) {
      buffer.write(limited[i]);
      if ((i == 2 || i == 4) && i != limited.length - 1) {
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
