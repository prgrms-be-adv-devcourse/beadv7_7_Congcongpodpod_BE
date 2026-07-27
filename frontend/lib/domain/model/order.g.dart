// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$OrderImpl _$$OrderImplFromJson(Map<String, dynamic> json) => _$OrderImpl(
      orderId: (json['orderId'] as num).toInt(),
      memberId: (json['memberId'] as num).toInt(),
      storeId: (json['storeId'] as num).toInt(),
      status: json['status'] as String,
      rejectReason: json['rejectReason'] as String?,
      paymentStatus: json['paymentStatus'] as String,
      phone: json['phone'] as String,
      dishId: (json['dishId'] as num).toInt(),
      dishName: json['dishName'] as String,
      quantity: (json['quantity'] as num).toInt(),
      unitPrice: json['unitPrice'] as num,
      totalPrice: json['totalPrice'] as num,
      pickupStartAt: json['pickupStartAt'] as String,
      pickupEndAt: json['pickupEndAt'] as String,
    );

Map<String, dynamic> _$$OrderImplToJson(_$OrderImpl instance) =>
    <String, dynamic>{
      'orderId': instance.orderId,
      'memberId': instance.memberId,
      'storeId': instance.storeId,
      'status': instance.status,
      'rejectReason': instance.rejectReason,
      'paymentStatus': instance.paymentStatus,
      'phone': instance.phone,
      'dishId': instance.dishId,
      'dishName': instance.dishName,
      'quantity': instance.quantity,
      'unitPrice': instance.unitPrice,
      'totalPrice': instance.totalPrice,
      'pickupStartAt': instance.pickupStartAt,
      'pickupEndAt': instance.pickupEndAt,
    };
