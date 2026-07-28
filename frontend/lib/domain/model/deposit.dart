import 'package:freezed_annotation/freezed_annotation.dart';

part 'deposit.freezed.dart';
part 'deposit.g.dart';

/// 예치금 잔액. 백엔드 `DepositBalanceResponse`(`GET /deposits/balance`)와 대응:
/// `{ memberId, balance }`. Auth/Deposit는 core-service 공통 래퍼(`{success,data,...}`)를
/// 안 쓰고 DTO를 그대로 반환한다(api-contracts.md 0절) — repository에서 그대로 파싱한다.
@freezed
class DepositBalance with _$DepositBalance {
  const factory DepositBalance({
    required int memberId,
    required num balance,
  }) = _DepositBalance;

  factory DepositBalance.fromJson(Map<String, Object?> json) =>
      _$DepositBalanceFromJson(json);
}

/// 예치금 내역 1건. 백엔드 `DepositHistoryResponse`(`GET /deposits/history`)와 대응:
/// `{ id, orderId, paymentId, type, amount, balanceAfter, createdAt }`.
/// `type`(`DepositType`: CHARGE/USE/REFUND)은 Store의 status와 같은 이유로 String 그대로.
@freezed
class DepositHistoryEntry with _$DepositHistoryEntry {
  const factory DepositHistoryEntry({
    required int id,
    int? orderId,
    int? paymentId,
    required String type,
    required num amount,
    required num balanceAfter,
    required String createdAt,
  }) = _DepositHistoryEntry;

  factory DepositHistoryEntry.fromJson(Map<String, Object?> json) =>
      _$DepositHistoryEntryFromJson(json);
}
