// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'payment.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$PaymentReadyImpl _$$PaymentReadyImplFromJson(Map<String, dynamic> json) =>
    _$PaymentReadyImpl(
      paymentId: (json['paymentId'] as num).toInt(),
      merchantOrderId: json['merchantOrderId'] as String,
      amount: json['amount'] as num,
      approvedStatus: json['approvedStatus'] as String,
      tossClientKey: json['tossClientKey'] as String,
    );

Map<String, dynamic> _$$PaymentReadyImplToJson(_$PaymentReadyImpl instance) =>
    <String, dynamic>{
      'paymentId': instance.paymentId,
      'merchantOrderId': instance.merchantOrderId,
      'amount': instance.amount,
      'approvedStatus': instance.approvedStatus,
      'tossClientKey': instance.tossClientKey,
    };

_$PaymentApproveImpl _$$PaymentApproveImplFromJson(Map<String, dynamic> json) =>
    _$PaymentApproveImpl(
      paymentId: (json['paymentId'] as num).toInt(),
      merchantOrderId: json['merchantOrderId'] as String,
      amount: json['amount'] as num,
      approvedStatus: json['approvedStatus'] as String,
      approvedAt: json['approvedAt'] as String,
      depositBalance: json['depositBalance'] as num,
    );

Map<String, dynamic> _$$PaymentApproveImplToJson(
  _$PaymentApproveImpl instance,
) => <String, dynamic>{
  'paymentId': instance.paymentId,
  'merchantOrderId': instance.merchantOrderId,
  'amount': instance.amount,
  'approvedStatus': instance.approvedStatus,
  'approvedAt': instance.approvedAt,
  'depositBalance': instance.depositBalance,
};
