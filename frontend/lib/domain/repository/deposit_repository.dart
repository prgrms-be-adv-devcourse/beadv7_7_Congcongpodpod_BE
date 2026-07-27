import '../model/deposit.dart';

/// 예치금 조회 기능 계약. 실제 dio 호출은 lib/data/repository/deposit_repository_impl.dart가 담당한다.
abstract interface class DepositRepository {
  /// 잔액 조회 (`GET /deposits/balance`).
  Future<DepositBalance> getBalance();

  /// 내역 조회 (`GET /deposits/history`, 최신순). 페이지네이션 UI는 아직 없어서
  /// 첫 페이지(기본 size=10)만 가져온다 — store_repository와 같은 이유.
  Future<List<DepositHistoryEntry>> getHistory();
}
