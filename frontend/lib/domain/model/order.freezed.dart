// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'order.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Order _$OrderFromJson(Map<String, dynamic> json) {
  return _Order.fromJson(json);
}

/// @nodoc
mixin _$Order {
  int get orderId => throw _privateConstructorUsedError;
  int get memberId => throw _privateConstructorUsedError;
  int get storeId =>
      throw _privateConstructorUsedError; // OrderStatus: RESERVED, PICKUP_READY, PICKED_UP, NO_SHOW, CANCELLED, REJECTED(ADR 014).
// Store/Dish의 status/dishStatus와 같은 이유로 String 그대로 받는다.
  String get status => throw _privateConstructorUsedError;
  String? get rejectReason => throw _privateConstructorUsedError;
  String get paymentStatus => throw _privateConstructorUsedError;
  String get phone => throw _privateConstructorUsedError;
  int get dishId => throw _privateConstructorUsedError;
  String get dishName => throw _privateConstructorUsedError;
  int get quantity => throw _privateConstructorUsedError;
  num get unitPrice => throw _privateConstructorUsedError;
  num get totalPrice =>
      throw _privateConstructorUsedError; // "HH:mm:ss" 문자열 그대로 — store.dart의 openTime/closeTime과 같은 이유.
// null 허용: 백엔드 Dish.create()가 pickupStartTime/pickupEndTime을 저장하는
// 코드 경로 자체가 없어서(2026-07-30 발견), 지금 생성되는 모든 주문의 이 값이
// 항상 null로 내려온다. required로 두면 Order.fromJson() 자체가 TypeError를
// 던져서 주문 성공 다이얼로그·주문목록이 통째로 깨진다 — 근본 수정(Dish 등록 시
// 픽업시간 저장)은 범위가 커서 일단 null을 그대로 받아들이게 완화한다.
  String? get pickupStartAt => throw _privateConstructorUsedError;
  String? get pickupEndAt => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $OrderCopyWith<Order> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $OrderCopyWith<$Res> {
  factory $OrderCopyWith(Order value, $Res Function(Order) then) =
      _$OrderCopyWithImpl<$Res, Order>;
  @useResult
  $Res call(
      {int orderId,
      int memberId,
      int storeId,
      String status,
      String? rejectReason,
      String paymentStatus,
      String phone,
      int dishId,
      String dishName,
      int quantity,
      num unitPrice,
      num totalPrice,
      String? pickupStartAt,
      String? pickupEndAt});
}

/// @nodoc
class _$OrderCopyWithImpl<$Res, $Val extends Order>
    implements $OrderCopyWith<$Res> {
  _$OrderCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? orderId = null,
    Object? memberId = null,
    Object? storeId = null,
    Object? status = null,
    Object? rejectReason = freezed,
    Object? paymentStatus = null,
    Object? phone = null,
    Object? dishId = null,
    Object? dishName = null,
    Object? quantity = null,
    Object? unitPrice = null,
    Object? totalPrice = null,
    Object? pickupStartAt = freezed,
    Object? pickupEndAt = freezed,
  }) {
    return _then(_value.copyWith(
      orderId: null == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int,
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      storeId: null == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      rejectReason: freezed == rejectReason
          ? _value.rejectReason
          : rejectReason // ignore: cast_nullable_to_non_nullable
              as String?,
      paymentStatus: null == paymentStatus
          ? _value.paymentStatus
          : paymentStatus // ignore: cast_nullable_to_non_nullable
              as String,
      phone: null == phone
          ? _value.phone
          : phone // ignore: cast_nullable_to_non_nullable
              as String,
      dishId: null == dishId
          ? _value.dishId
          : dishId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      quantity: null == quantity
          ? _value.quantity
          : quantity // ignore: cast_nullable_to_non_nullable
              as int,
      unitPrice: null == unitPrice
          ? _value.unitPrice
          : unitPrice // ignore: cast_nullable_to_non_nullable
              as num,
      totalPrice: null == totalPrice
          ? _value.totalPrice
          : totalPrice // ignore: cast_nullable_to_non_nullable
              as num,
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
abstract class _$$OrderImplCopyWith<$Res> implements $OrderCopyWith<$Res> {
  factory _$$OrderImplCopyWith(
          _$OrderImpl value, $Res Function(_$OrderImpl) then) =
      __$$OrderImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int orderId,
      int memberId,
      int storeId,
      String status,
      String? rejectReason,
      String paymentStatus,
      String phone,
      int dishId,
      String dishName,
      int quantity,
      num unitPrice,
      num totalPrice,
      String? pickupStartAt,
      String? pickupEndAt});
}

/// @nodoc
class __$$OrderImplCopyWithImpl<$Res>
    extends _$OrderCopyWithImpl<$Res, _$OrderImpl>
    implements _$$OrderImplCopyWith<$Res> {
  __$$OrderImplCopyWithImpl(
      _$OrderImpl _value, $Res Function(_$OrderImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? orderId = null,
    Object? memberId = null,
    Object? storeId = null,
    Object? status = null,
    Object? rejectReason = freezed,
    Object? paymentStatus = null,
    Object? phone = null,
    Object? dishId = null,
    Object? dishName = null,
    Object? quantity = null,
    Object? unitPrice = null,
    Object? totalPrice = null,
    Object? pickupStartAt = freezed,
    Object? pickupEndAt = freezed,
  }) {
    return _then(_$OrderImpl(
      orderId: null == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int,
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      storeId: null == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      rejectReason: freezed == rejectReason
          ? _value.rejectReason
          : rejectReason // ignore: cast_nullable_to_non_nullable
              as String?,
      paymentStatus: null == paymentStatus
          ? _value.paymentStatus
          : paymentStatus // ignore: cast_nullable_to_non_nullable
              as String,
      phone: null == phone
          ? _value.phone
          : phone // ignore: cast_nullable_to_non_nullable
              as String,
      dishId: null == dishId
          ? _value.dishId
          : dishId // ignore: cast_nullable_to_non_nullable
              as int,
      dishName: null == dishName
          ? _value.dishName
          : dishName // ignore: cast_nullable_to_non_nullable
              as String,
      quantity: null == quantity
          ? _value.quantity
          : quantity // ignore: cast_nullable_to_non_nullable
              as int,
      unitPrice: null == unitPrice
          ? _value.unitPrice
          : unitPrice // ignore: cast_nullable_to_non_nullable
              as num,
      totalPrice: null == totalPrice
          ? _value.totalPrice
          : totalPrice // ignore: cast_nullable_to_non_nullable
              as num,
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
class _$OrderImpl implements _Order {
  const _$OrderImpl(
      {required this.orderId,
      required this.memberId,
      required this.storeId,
      required this.status,
      this.rejectReason,
      required this.paymentStatus,
      required this.phone,
      required this.dishId,
      required this.dishName,
      required this.quantity,
      required this.unitPrice,
      required this.totalPrice,
      this.pickupStartAt,
      this.pickupEndAt});

  factory _$OrderImpl.fromJson(Map<String, dynamic> json) =>
      _$$OrderImplFromJson(json);

  @override
  final int orderId;
  @override
  final int memberId;
  @override
  final int storeId;
// OrderStatus: RESERVED, PICKUP_READY, PICKED_UP, NO_SHOW, CANCELLED, REJECTED(ADR 014).
// Store/Dish의 status/dishStatus와 같은 이유로 String 그대로 받는다.
  @override
  final String status;
  @override
  final String? rejectReason;
  @override
  final String paymentStatus;
  @override
  final String phone;
  @override
  final int dishId;
  @override
  final String dishName;
  @override
  final int quantity;
  @override
  final num unitPrice;
  @override
  final num totalPrice;
// "HH:mm:ss" 문자열 그대로 — store.dart의 openTime/closeTime과 같은 이유.
// null 허용: 백엔드 Dish.create()가 pickupStartTime/pickupEndTime을 저장하는
// 코드 경로 자체가 없어서(2026-07-30 발견), 지금 생성되는 모든 주문의 이 값이
// 항상 null로 내려온다. required로 두면 Order.fromJson() 자체가 TypeError를
// 던져서 주문 성공 다이얼로그·주문목록이 통째로 깨진다 — 근본 수정(Dish 등록 시
// 픽업시간 저장)은 범위가 커서 일단 null을 그대로 받아들이게 완화한다.
  @override
  final String? pickupStartAt;
  @override
  final String? pickupEndAt;

  @override
  String toString() {
    return 'Order(orderId: $orderId, memberId: $memberId, storeId: $storeId, status: $status, rejectReason: $rejectReason, paymentStatus: $paymentStatus, phone: $phone, dishId: $dishId, dishName: $dishName, quantity: $quantity, unitPrice: $unitPrice, totalPrice: $totalPrice, pickupStartAt: $pickupStartAt, pickupEndAt: $pickupEndAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$OrderImpl &&
            (identical(other.orderId, orderId) || other.orderId == orderId) &&
            (identical(other.memberId, memberId) ||
                other.memberId == memberId) &&
            (identical(other.storeId, storeId) || other.storeId == storeId) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.rejectReason, rejectReason) ||
                other.rejectReason == rejectReason) &&
            (identical(other.paymentStatus, paymentStatus) ||
                other.paymentStatus == paymentStatus) &&
            (identical(other.phone, phone) || other.phone == phone) &&
            (identical(other.dishId, dishId) || other.dishId == dishId) &&
            (identical(other.dishName, dishName) ||
                other.dishName == dishName) &&
            (identical(other.quantity, quantity) ||
                other.quantity == quantity) &&
            (identical(other.unitPrice, unitPrice) ||
                other.unitPrice == unitPrice) &&
            (identical(other.totalPrice, totalPrice) ||
                other.totalPrice == totalPrice) &&
            (identical(other.pickupStartAt, pickupStartAt) ||
                other.pickupStartAt == pickupStartAt) &&
            (identical(other.pickupEndAt, pickupEndAt) ||
                other.pickupEndAt == pickupEndAt));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      orderId,
      memberId,
      storeId,
      status,
      rejectReason,
      paymentStatus,
      phone,
      dishId,
      dishName,
      quantity,
      unitPrice,
      totalPrice,
      pickupStartAt,
      pickupEndAt);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$OrderImplCopyWith<_$OrderImpl> get copyWith =>
      __$$OrderImplCopyWithImpl<_$OrderImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$OrderImplToJson(
      this,
    );
  }
}

abstract class _Order implements Order {
  const factory _Order(
      {required final int orderId,
      required final int memberId,
      required final int storeId,
      required final String status,
      final String? rejectReason,
      required final String paymentStatus,
      required final String phone,
      required final int dishId,
      required final String dishName,
      required final int quantity,
      required final num unitPrice,
      required final num totalPrice,
      final String? pickupStartAt,
      final String? pickupEndAt}) = _$OrderImpl;

  factory _Order.fromJson(Map<String, dynamic> json) = _$OrderImpl.fromJson;

  @override
  int get orderId;
  @override
  int get memberId;
  @override
  int get storeId;
  @override // OrderStatus: RESERVED, PICKUP_READY, PICKED_UP, NO_SHOW, CANCELLED, REJECTED(ADR 014).
// Store/Dish의 status/dishStatus와 같은 이유로 String 그대로 받는다.
  String get status;
  @override
  String? get rejectReason;
  @override
  String get paymentStatus;
  @override
  String get phone;
  @override
  int get dishId;
  @override
  String get dishName;
  @override
  int get quantity;
  @override
  num get unitPrice;
  @override
  num get totalPrice;
  @override // "HH:mm:ss" 문자열 그대로 — store.dart의 openTime/closeTime과 같은 이유.
// null 허용: 백엔드 Dish.create()가 pickupStartTime/pickupEndTime을 저장하는
// 코드 경로 자체가 없어서(2026-07-30 발견), 지금 생성되는 모든 주문의 이 값이
// 항상 null로 내려온다. required로 두면 Order.fromJson() 자체가 TypeError를
// 던져서 주문 성공 다이얼로그·주문목록이 통째로 깨진다 — 근본 수정(Dish 등록 시
// 픽업시간 저장)은 범위가 커서 일단 null을 그대로 받아들이게 완화한다.
  String? get pickupStartAt;
  @override
  String? get pickupEndAt;
  @override
  @JsonKey(ignore: true)
  _$$OrderImplCopyWith<_$OrderImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
