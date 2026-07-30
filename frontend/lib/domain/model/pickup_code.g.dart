// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pickup_code.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$PickupCodeImpl _$$PickupCodeImplFromJson(Map<String, dynamic> json) =>
    _$PickupCodeImpl(
      orderId: (json['orderId'] as num).toInt(),
      dishName: json['dishName'] as String,
      pickupCode: json['pickupCode'] as String,
      pickupStartAt: json['pickupStartAt'] as String?,
      pickupEndAt: json['pickupEndAt'] as String?,
    );

Map<String, dynamic> _$$PickupCodeImplToJson(_$PickupCodeImpl instance) =>
    <String, dynamic>{
      'orderId': instance.orderId,
      'dishName': instance.dishName,
      'pickupCode': instance.pickupCode,
      'pickupStartAt': instance.pickupStartAt,
      'pickupEndAt': instance.pickupEndAt,
    };
