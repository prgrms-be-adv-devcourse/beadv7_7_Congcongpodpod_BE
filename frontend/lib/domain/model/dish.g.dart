// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dish.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$DishImpl _$$DishImplFromJson(Map<String, dynamic> json) => _$DishImpl(
      dishId: (json['dishId'] as num).toInt(),
      dishName: json['dishName'] as String,
      registeredAt: json['registeredAt'] as String,
      description: json['description'] as String?,
      thumbnailUrl: json['thumbnailUrl'] as String?,
      stockQuantity: (json['stockQuantity'] as num).toInt(),
      dishPrice: json['dishPrice'] as num,
      discountPrice: json['discountPrice'] as num,
      storeId: (json['storeId'] as num?)?.toInt(),
      dishStatus: json['dishStatus'] as String?,
    );

Map<String, dynamic> _$$DishImplToJson(_$DishImpl instance) =>
    <String, dynamic>{
      'dishId': instance.dishId,
      'dishName': instance.dishName,
      'registeredAt': instance.registeredAt,
      'description': instance.description,
      'thumbnailUrl': instance.thumbnailUrl,
      'stockQuantity': instance.stockQuantity,
      'dishPrice': instance.dishPrice,
      'discountPrice': instance.discountPrice,
      'storeId': instance.storeId,
      'dishStatus': instance.dishStatus,
    };
