// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'pickup_code.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

PickupCode _$PickupCodeFromJson(Map<String, dynamic> json) {
  return _PickupCode.fromJson(json);
}

/// @nodoc
mixin _$PickupCode {
  int get orderId => throw _privateConstructorUsedError;
  String get dishName => throw _privateConstructorUsedError;
  String get pickupCode =>
      throw _privateConstructorUsedError; // order.dart와 같은 이유(2026-07-30) — 백엔드가 Dish 등록 시 픽업시간을 저장하지
// 않아 항상 null로 내려온다. null 허용으로 완화.
  String? get pickupStartAt => throw _privateConstructorUsedError;
  String? get pickupEndAt => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $PickupCodeCopyWith<PickupCode> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PickupCodeCopyWith<$Res> {
  factory $PickupCodeCopyWith(
          PickupCode value, $Res Function(PickupCode) then) =
      _$PickupCodeCopyWithImpl<$Res, PickupCode>;
  @useResult
  $Res call(
      {int orderId,
      String dishName,
      String pickupCode,
      String? pickupStartAt,
      String? pickupEndAt});
}

/// @nodoc
class _$PickupCodeCopyWithImpl<$Res, $Val extends PickupCode>
    implements $PickupCodeCopyWith<$Res> {
  _$PickupCodeCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? orderId = null,
    Object? dishName = null,
    Object? pickupCode = null,
    Object? pickupStartAt = freezed,
    Object? pickupEndAt = freezed,
  }) {
    return _then(_value.copyWith(
      orderId: null == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      pickupCode: null == pickupCode
          ? _value.pickupCode
          : pickupCode // ignore: cast_nullable_to_non_nullable
              as String,
      pickupStartAt: freezed == pickupStartAt
          ? _value.pickupStartAt
          : pickupStartAt // ignore: cast_nullable_to_non_nullable
              as String?,
      pickupEndAt: freezed == pickupEndAt
          ? _value.pickupEndAt
          : pickupEndAt // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PickupCodeImplCopyWith<$Res>
    implements $PickupCodeCopyWith<$Res> {
  factory _$$PickupCodeImplCopyWith(
          _$PickupCodeImpl value, $Res Function(_$PickupCodeImpl) then) =
      __$$PickupCodeImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int orderId,
      String dishName,
      String pickupCode,
      String? pickupStartAt,
      String? pickupEndAt});
}

/// @nodoc
class __$$PickupCodeImplCopyWithImpl<$Res>
    extends _$PickupCodeCopyWithImpl<$Res, _$PickupCodeImpl>
    implements _$$PickupCodeImplCopyWith<$Res> {
  __$$PickupCodeImplCopyWithImpl(
      _$PickupCodeImpl _value, $Res Function(_$PickupCodeImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? orderId = null,
    Object? dishName = null,
    Object? pickupCode = null,
    Object? pickupStartAt = freezed,
    Object? pickupEndAt = freezed,
  }) {
    return _then(_$PickupCodeImpl(
      orderId: null == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      pickupCode: null == pickupCode
          ? _value.pickupCode
          : pickupCode // ignore: cast_nullable_to_non_nullable
              as String,
      pickupStartAt: freezed == pickupStartAt
          ? _value.pickupStartAt
          : pickupStartAt // ignore: cast_nullable_to_non_nullable
              as String?,
      pickupEndAt: freezed == pickupEndAt
          ? _value.pickupEndAt
          : pickupEndAt // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PickupCodeImpl implements _PickupCode {
  const _$PickupCodeImpl(
      {required this.orderId,
      required this.dishName,
      required this.pickupCode,
      this.pickupStartAt,
      this.pickupEndAt});

  factory _$PickupCodeImpl.fromJson(Map<String, dynamic> json) =>
      _$$PickupCodeImplFromJson(json);

  @override
  final int orderId;
  @override
  final String dishName;
  @override
  final String pickupCode;
// order.dart와 같은 이유(2026-07-30) — 백엔드가 Dish 등록 시 픽업시간을 저장하지
// 않아 항상 null로 내려온다. null 허용으로 완화.
  @override
  final String? pickupStartAt;
  @override
  final String? pickupEndAt;

  @override
  String toString() {
    return 'PickupCode(orderId: $orderId, dishName: $dishName, pickupCode: $pickupCode, pickupStartAt: $pickupStartAt, pickupEndAt: $pickupEndAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PickupCodeImpl &&
            (identical(other.orderId, orderId) || other.orderId == orderId) &&
            (identical(other.dishName, dishName) ||
                other.dishName == dishName) &&
            (identical(other.pickupCode, pickupCode) ||
                other.pickupCode == pickupCode) &&
            (identical(other.pickupStartAt, pickupStartAt) ||
                other.pickupStartAt == pickupStartAt) &&
            (identical(other.pickupEndAt, pickupEndAt) ||
                other.pickupEndAt == pickupEndAt));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType, orderId, dishName, pickupCode, pickupStartAt, pickupEndAt);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$PickupCodeImplCopyWith<_$PickupCodeImpl> get copyWith =>
      __$$PickupCodeImplCopyWithImpl<_$PickupCodeImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PickupCodeImplToJson(
      this,
    );
  }
}

abstract class _PickupCode implements PickupCode {
  const factory _PickupCode(
      {required final int orderId,
      required final String dishName,
      required final String pickupCode,
      final String? pickupStartAt,
      final String? pickupEndAt}) = _$PickupCodeImpl;

  factory _PickupCode.fromJson(Map<String, dynamic> json) =
      _$PickupCodeImpl.fromJson;

  @override
  int get orderId;
  @override
  String get dishName;
  @override
  String get pickupCode;
  @override // order.dart와 같은 이유(2026-07-30) — 백엔드가 Dish 등록 시 픽업시간을 저장하지
// 않아 항상 null로 내려온다. null 허용으로 완화.
  String? get pickupStartAt;
  @override
  String? get pickupEndAt;
  @override
  @JsonKey(ignore: true)
  _$$PickupCodeImplCopyWith<_$PickupCodeImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
