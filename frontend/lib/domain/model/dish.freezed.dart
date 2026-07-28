// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'dish.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Dish _$DishFromJson(Map<String, dynamic> json) {
  return _Dish.fromJson(json);
}

/// @nodoc
mixin _$Dish {
  int get dishId => throw _privateConstructorUsedError;
  String get dishName => throw _privateConstructorUsedError;
  String get registeredAt => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  String? get thumbnailUrl => throw _privateConstructorUsedError;
  int get stockQuantity => throw _privateConstructorUsedError;
  num get dishPrice => throw _privateConstructorUsedError;
  num get discountPrice =>
      throw _privateConstructorUsedError; // `GET /stores/nearby`가 임베딩하는 StoreDishResponse엔 없고, `GET /dishes/{dishId}`
// 단건 조회(전체 DishResponse)에만 있다 — 주문 생성(POST /orders)에 storeId가
// 필요해서(checkout_screen.dart), 단건 조회로 다시 가져올 때만 채워진다.
  int? get storeId =>
      throw _privateConstructorUsedError; // 판매상태(ON_SALE/SOLD_OUT/CLOSED/EXPIRED). storeId와 같은 이유로 nullable —
// `GET /stores/nearby`(StoreDishResponse)엔 없고, 셀러 상품관리 화면(S2, `getEachStoreDishes`)
// 응답엔 있다(둘 다 DishResponse를 축소/그대로 쓰는 차이).
  String? get dishStatus => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $DishCopyWith<Dish> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DishCopyWith<$Res> {
  factory $DishCopyWith(Dish value, $Res Function(Dish) then) =
      _$DishCopyWithImpl<$Res, Dish>;
  @useResult
  $Res call(
      {int dishId,
      String dishName,
      String registeredAt,
      String? description,
      String? thumbnailUrl,
      int stockQuantity,
      num dishPrice,
      num discountPrice,
      int? storeId,
      String? dishStatus});
}

/// @nodoc
class _$DishCopyWithImpl<$Res, $Val extends Dish>
    implements $DishCopyWith<$Res> {
  _$DishCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dishId = null,
    Object? dishName = null,
    Object? registeredAt = null,
    Object? description = freezed,
    Object? thumbnailUrl = freezed,
    Object? stockQuantity = null,
    Object? dishPrice = null,
    Object? discountPrice = null,
    Object? storeId = freezed,
    Object? dishStatus = freezed,
  }) {
    return _then(_value.copyWith(
      dishId: null == dishId
          ? _value.dishId
          : dishId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      registeredAt: null == registeredAt
          ? _value.registeredAt
          : registeredAt // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      stockQuantity: null == stockQuantity
          ? _value.stockQuantity
          : stockQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      dishPrice: null == dishPrice
          ? _value.dishPrice
          : dishPrice // ignore: cast_nullable_to_non_nullable
              as num,
      discountPrice: null == discountPrice
          ? _value.discountPrice
          : discountPrice // ignore: cast_nullable_to_non_nullable
              as num,
      storeId: freezed == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int?,
      dishStatus: freezed == dishStatus
          ? _value.dishStatus
          : dishStatus // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DishImplCopyWith<$Res> implements $DishCopyWith<$Res> {
  factory _$$DishImplCopyWith(
          _$DishImpl value, $Res Function(_$DishImpl) then) =
      __$$DishImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int dishId,
      String dishName,
      String registeredAt,
      String? description,
      String? thumbnailUrl,
      int stockQuantity,
      num dishPrice,
      num discountPrice,
      int? storeId,
      String? dishStatus});
}

/// @nodoc
class __$$DishImplCopyWithImpl<$Res>
    extends _$DishCopyWithImpl<$Res, _$DishImpl>
    implements _$$DishImplCopyWith<$Res> {
  __$$DishImplCopyWithImpl(_$DishImpl _value, $Res Function(_$DishImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? dishId = null,
    Object? dishName = null,
    Object? registeredAt = null,
    Object? description = freezed,
    Object? thumbnailUrl = freezed,
    Object? stockQuantity = null,
    Object? dishPrice = null,
    Object? discountPrice = null,
    Object? storeId = freezed,
    Object? dishStatus = freezed,
  }) {
    return _then(_$DishImpl(
      dishId: null == dishId
          ? _value.dishId
          : dishId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      registeredAt: null == registeredAt
          ? _value.registeredAt
          : registeredAt // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      thumbnailUrl: freezed == thumbnailUrl
          ? _value.thumbnailUrl
          : thumbnailUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      stockQuantity: null == stockQuantity
          ? _value.stockQuantity
          : stockQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      dishPrice: null == dishPrice
          ? _value.dishPrice
          : dishPrice // ignore: cast_nullable_to_non_nullable
              as num,
      discountPrice: null == discountPrice
          ? _value.discountPrice
          : discountPrice // ignore: cast_nullable_to_non_nullable
              as num,
      storeId: freezed == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int?,
      dishStatus: freezed == dishStatus
          ? _value.dishStatus
          : dishStatus // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DishImpl implements _Dish {
  const _$DishImpl(
      {required this.dishId,
      required this.dishName,
      required this.registeredAt,
      this.description,
      this.thumbnailUrl,
      required this.stockQuantity,
      required this.dishPrice,
      required this.discountPrice,
      this.storeId,
      this.dishStatus});

  factory _$DishImpl.fromJson(Map<String, dynamic> json) =>
      _$$DishImplFromJson(json);

  @override
  final int dishId;
  @override
  final String dishName;
  @override
  final String registeredAt;
  @override
  final String? description;
  @override
  final String? thumbnailUrl;
  @override
  final int stockQuantity;
  @override
  final num dishPrice;
  @override
  final num discountPrice;
// `GET /stores/nearby`가 임베딩하는 StoreDishResponse엔 없고, `GET /dishes/{dishId}`
// 단건 조회(전체 DishResponse)에만 있다 — 주문 생성(POST /orders)에 storeId가
// 필요해서(checkout_screen.dart), 단건 조회로 다시 가져올 때만 채워진다.
  @override
  final int? storeId;
// 판매상태(ON_SALE/SOLD_OUT/CLOSED/EXPIRED). storeId와 같은 이유로 nullable —
// `GET /stores/nearby`(StoreDishResponse)엔 없고, 셀러 상품관리 화면(S2, `getEachStoreDishes`)
// 응답엔 있다(둘 다 DishResponse를 축소/그대로 쓰는 차이).
  @override
  final String? dishStatus;

  @override
  String toString() {
    return 'Dish(dishId: $dishId, dishName: $dishName, registeredAt: $registeredAt, description: $description, thumbnailUrl: $thumbnailUrl, stockQuantity: $stockQuantity, dishPrice: $dishPrice, discountPrice: $discountPrice, storeId: $storeId, dishStatus: $dishStatus)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DishImpl &&
            (identical(other.dishId, dishId) || other.dishId == dishId) &&
            (identical(other.dishName, dishName) ||
                other.dishName == dishName) &&
            (identical(other.registeredAt, registeredAt) ||
                other.registeredAt == registeredAt) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.thumbnailUrl, thumbnailUrl) ||
                other.thumbnailUrl == thumbnailUrl) &&
            (identical(other.stockQuantity, stockQuantity) ||
                other.stockQuantity == stockQuantity) &&
            (identical(other.dishPrice, dishPrice) ||
                other.dishPrice == dishPrice) &&
            (identical(other.discountPrice, discountPrice) ||
                other.discountPrice == discountPrice) &&
            (identical(other.storeId, storeId) || other.storeId == storeId) &&
            (identical(other.dishStatus, dishStatus) ||
                other.dishStatus == dishStatus));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      dishId,
      dishName,
      registeredAt,
      description,
      thumbnailUrl,
      stockQuantity,
      dishPrice,
      discountPrice,
      storeId,
      dishStatus);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$DishImplCopyWith<_$DishImpl> get copyWith =>
      __$$DishImplCopyWithImpl<_$DishImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DishImplToJson(
      this,
    );
  }
}

abstract class _Dish implements Dish {
  const factory _Dish(
      {required final int dishId,
      required final String dishName,
      required final String registeredAt,
      final String? description,
      final String? thumbnailUrl,
      required final int stockQuantity,
      required final num dishPrice,
      required final num discountPrice,
      final int? storeId,
      final String? dishStatus}) = _$DishImpl;

  factory _Dish.fromJson(Map<String, dynamic> json) = _$DishImpl.fromJson;

  @override
  int get dishId;
  @override
  String get dishName;
  @override
  String get registeredAt;
  @override
  String? get description;
  @override
  String? get thumbnailUrl;
  @override
  int get stockQuantity;
  @override
  num get dishPrice;
  @override
  num get discountPrice;
  @override // `GET /stores/nearby`가 임베딩하는 StoreDishResponse엔 없고, `GET /dishes/{dishId}`
// 단건 조회(전체 DishResponse)에만 있다 — 주문 생성(POST /orders)에 storeId가
// 필요해서(checkout_screen.dart), 단건 조회로 다시 가져올 때만 채워진다.
  int? get storeId;
  @override // 판매상태(ON_SALE/SOLD_OUT/CLOSED/EXPIRED). storeId와 같은 이유로 nullable —
// `GET /stores/nearby`(StoreDishResponse)엔 없고, 셀러 상품관리 화면(S2, `getEachStoreDishes`)
// 응답엔 있다(둘 다 DishResponse를 축소/그대로 쓰는 차이).
  String? get dishStatus;
  @override
  @JsonKey(ignore: true)
  _$$DishImplCopyWith<_$DishImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
