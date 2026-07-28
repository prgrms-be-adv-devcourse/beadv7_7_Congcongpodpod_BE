// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deposit.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$DepositBalanceImpl _$$DepositBalanceImplFromJson(Map<String, dynamic> json) =>
    _$DepositBalanceImpl(
      memberId: (json['memberId'] as num).toInt(),
      balance: json['balance'] as num,
    );

Map<String, dynamic> _$$DepositBalanceImplToJson(
        _$DepositBalanceImpl instance) =>
    <String, dynamic>{
      'memberId': instance.memberId,
      'balance': instance.balance,
    };

_$DepositHistoryEntryImpl _$$DepositHistoryEntryImplFromJson(
        Map<String, dynamic> json) =>
    _$DepositHistoryEntryImpl(
      id: (json['id'] as num).toInt(),
      orderId: (json['orderId'] as num?)?.toInt(),
      paymentId: (json['paymentId'] as num?)?.toInt(),
      type: json['type'] as String,
      amount: json['amount'] as num,
      balanceAfter: json['balanceAfter'] as num,
      createdAt: json['createdAt'] as String,
    );

Map<String, dynamic> _$$DepositHistoryEntryImplToJson(
        _$DepositHistoryEntryImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'orderId': instance.orderId,
      'paymentId': instance.paymentId,
      'type': instance.type,
      'amount': instance.amount,
      'balanceAfter': instance.balanceAfter,
      'createdAt': instance.createdAt,
    };
