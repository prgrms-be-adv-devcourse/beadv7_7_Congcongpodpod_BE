// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'store.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$StoreImpl _$$StoreImplFromJson(Map<String, dynamic> json) => _$StoreImpl(
  storeId: (json['storeId'] as num).toInt(),
  storeName: json['storeName'] as String,
  storeAddress: json['storeAddress'] as String,
  storePhone: json['storePhone'] as String,
  openTime: json['openTime'] as String,
  closeTime: json['closeTime'] as String,
  latitude: (json['latitude'] as num).toDouble(),
  longitude: (json['longitude'] as num).toDouble(),
  holidays: (json['holidays'] as List<dynamic>?)
      ?.map((e) => e as String)
      .toList(),
  status: json['status'] as String,
);

Map<String, dynamic> _$$StoreImplToJson(_$StoreImpl instance) =>
    <String, dynamic>{
      'storeId': instance.storeId,
      'storeName': instance.storeName,
      'storeAddress': instance.storeAddress,
      'storePhone': instance.storePhone,
      'openTime': instance.openTime,
      'closeTime': instance.closeTime,
      'latitude': instance.latitude,
      'longitude': instance.longitude,
      'holidays': instance.holidays,
      'status': instance.status,
    };
