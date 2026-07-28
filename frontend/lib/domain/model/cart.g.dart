// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'cart.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$CartItemImpl _$$CartItemImplFromJson(Map<String, dynamic> json) =>
    _$CartItemImpl(
      cartItemId: (json['cartItemId'] as num).toInt(),
      dishId: (json['dishId'] as num).toInt(),
      dishName: json['dishName'] as String,
      unitPrice: json['unitPrice'] as num,
      quantity: (json['quantity'] as num).toInt(),
      subtotalPrice: json['subtotalPrice'] as num,
    );

Map<String, dynamic> _$$CartItemImplToJson(_$CartItemImpl instance) =>
    <String, dynamic>{
      'cartItemId': instance.cartItemId,
      'dishId': instance.dishId,
      'dishName': instance.dishName,
      'unitPrice': instance.unitPrice,
      'quantity': instance.quantity,
      'subtotalPrice': instance.subtotalPrice,
    };

_$CartImpl _$$CartImplFromJson(Map<String, dynamic> json) => _$CartImpl(
      cartId: (json['cartId'] as num).toInt(),
      memberId: (json['memberId'] as num).toInt(),
      items: (json['items'] as List<dynamic>)
          .map((e) => CartItem.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalPrice: json['totalPrice'] as num,
    );

Map<String, dynamic> _$$CartImplToJson(_$CartImpl instance) =>
    <String, dynamic>{
      'cartId': instance.cartId,
      'memberId': instance.memberId,
      'items': instance.items,
      'totalPrice': instance.totalPrice,
    };
