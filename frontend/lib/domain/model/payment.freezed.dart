// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'payment.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
  'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models',
);

PaymentReady _$PaymentReadyFromJson(Map<String, dynamic> json) {
  return _PaymentReady.fromJson(json);
}

/// @nodoc
mixin _$PaymentReady {
  int get paymentId => throw _privateConstructorUsedError;
  String get merchantOrderId => throw _privateConstructorUsedError;
  num get amount => throw _privateConstructorUsedError;
  String get approvedStatus => throw _privateConstructorUsedError;
  String get tossClientKey => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $PaymentReadyCopyWith<PaymentReady> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PaymentReadyCopyWith<$Res> {
  factory $PaymentReadyCopyWith(
    PaymentReady value,
    $Res Function(PaymentReady) then,
  ) = _$PaymentReadyCopyWithImpl<$Res, PaymentReady>;
  @useResult
  $Res call({
    int paymentId,
    String merchantOrderId,
    num amount,
    String approvedStatus,
    String tossClientKey,
  });
}

/// @nodoc
class _$PaymentReadyCopyWithImpl<$Res, $Val extends PaymentReady>
    implements $PaymentReadyCopyWith<$Res> {
  _$PaymentReadyCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? paymentId = null,
    Object? merchantOrderId = null,
    Object? amount = null,
    Object? approvedStatus = null,
    Object? tossClientKey = null,
  }) {
    return _then(
      _value.copyWith(
            paymentId: null == paymentId
                ? _value.paymentId
                : paymentId // ignore: cast_nullable_to_non_nullable
                      as int,
            merchantOrderId: null == merchantOrderId
                ? _value.merchantOrderId
                : merchantOrderId // ignore: cast_nullable_to_non_nullable
                      as String,
            amount: null == amount
                ? _value.amount
                : amount // ignore: cast_nullable_to_non_nullable
                      as num,
            approvedStatus: null == approvedStatus
                ? _value.approvedStatus
                : approvedStatus // ignore: cast_nullable_to_non_nullable
                      as String,
            tossClientKey: null == tossClientKey
                ? _value.tossClientKey
                : tossClientKey // ignore: cast_nullable_to_non_nullable
                      as String,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$PaymentReadyImplCopyWith<$Res>
    implements $PaymentReadyCopyWith<$Res> {
  factory _$$PaymentReadyImplCopyWith(
    _$PaymentReadyImpl value,
    $Res Function(_$PaymentReadyImpl) then,
  ) = __$$PaymentReadyImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    int paymentId,
    String merchantOrderId,
    num amount,
    String approvedStatus,
    String tossClientKey,
  });
}

/// @nodoc
class __$$PaymentReadyImplCopyWithImpl<$Res>
    extends _$PaymentReadyCopyWithImpl<$Res, _$PaymentReadyImpl>
    implements _$$PaymentReadyImplCopyWith<$Res> {
  __$$PaymentReadyImplCopyWithImpl(
    _$PaymentReadyImpl _value,
    $Res Function(_$PaymentReadyImpl) _then,
  ) : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? paymentId = null,
    Object? merchantOrderId = null,
    Object? amount = null,
    Object? approvedStatus = null,
    Object? tossClientKey = null,
  }) {
    return _then(
      _$PaymentReadyImpl(
        paymentId: null == paymentId
            ? _value.paymentId
            : paymentId // ignore: cast_nullable_to_non_nullable
                  as int,
        merchantOrderId: null == merchantOrderId
            ? _value.merchantOrderId
            : merchantOrderId // ignore: cast_nullable_to_non_nullable
                  as String,
        amount: null == amount
            ? _value.amount
            : amount // ignore: cast_nullable_to_non_nullable
                  as num,
        approvedStatus: null == approvedStatus
            ? _value.approvedStatus
            : approvedStatus // ignore: cast_nullable_to_non_nullable
                  as String,
        tossClientKey: null == tossClientKey
            ? _value.tossClientKey
            : tossClientKey // ignore: cast_nullable_to_non_nullable
                  as String,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$PaymentReadyImpl implements _PaymentReady {
  const _$PaymentReadyImpl({
    required this.paymentId,
    required this.merchantOrderId,
    required this.amount,
    required this.approvedStatus,
    required this.tossClientKey,
  });

  factory _$PaymentReadyImpl.fromJson(Map<String, dynamic> json) =>
      _$$PaymentReadyImplFromJson(json);

  @override
  final int paymentId;
  @override
  final String merchantOrderId;
  @override
  final num amount;
  @override
  final String approvedStatus;
  @override
  final String tossClientKey;

  @override
  String toString() {
    return 'PaymentReady(paymentId: $paymentId, merchantOrderId: $merchantOrderId, amount: $amount, approvedStatus: $approvedStatus, tossClientKey: $tossClientKey)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PaymentReadyImpl &&
            (identical(other.paymentId, paymentId) ||
                other.paymentId == paymentId) &&
            (identical(other.merchantOrderId, merchantOrderId) ||
                other.merchantOrderId == merchantOrderId) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.approvedStatus, approvedStatus) ||
                other.approvedStatus == approvedStatus) &&
            (identical(other.tossClientKey, tossClientKey) ||
                other.tossClientKey == tossClientKey));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    paymentId,
    merchantOrderId,
    amount,
    approvedStatus,
    tossClientKey,
  );

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$PaymentReadyImplCopyWith<_$PaymentReadyImpl> get copyWith =>
      __$$PaymentReadyImplCopyWithImpl<_$PaymentReadyImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PaymentReadyImplToJson(this);
  }
}

abstract class _PaymentReady implements PaymentReady {
  const factory _PaymentReady({
    required final int paymentId,
    required final String merchantOrderId,
    required final num amount,
    required final String approvedStatus,
    required final String tossClientKey,
  }) = _$PaymentReadyImpl;

  factory _PaymentReady.fromJson(Map<String, dynamic> json) =
      _$PaymentReadyImpl.fromJson;

  @override
  int get paymentId;
  @override
  String get merchantOrderId;
  @override
  num get amount;
  @override
  String get approvedStatus;
  @override
  String get tossClientKey;
  @override
  @JsonKey(ignore: true)
  _$$PaymentReadyImplCopyWith<_$PaymentReadyImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

PaymentApprove _$PaymentApproveFromJson(Map<String, dynamic> json) {
  return _PaymentApprove.fromJson(json);
}

/// @nodoc
mixin _$PaymentApprove {
  int get paymentId => throw _privateConstructorUsedError;
  String get merchantOrderId => throw _privateConstructorUsedError;
  num get amount => throw _privateConstructorUsedError;
  String get approvedStatus => throw _privateConstructorUsedError;
  String get approvedAt => throw _privateConstructorUsedError;
  num get depositBalance => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $PaymentApproveCopyWith<PaymentApprove> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PaymentApproveCopyWith<$Res> {
  factory $PaymentApproveCopyWith(
    PaymentApprove value,
    $Res Function(PaymentApprove) then,
  ) = _$PaymentApproveCopyWithImpl<$Res, PaymentApprove>;
  @useResult
  $Res call({
    int paymentId,
    String merchantOrderId,
    num amount,
    String approvedStatus,
    String approvedAt,
    num depositBalance,
  });
}

/// @nodoc
class _$PaymentApproveCopyWithImpl<$Res, $Val extends PaymentApprove>
    implements $PaymentApproveCopyWith<$Res> {
  _$PaymentApproveCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? paymentId = null,
    Object? merchantOrderId = null,
    Object? amount = null,
    Object? approvedStatus = null,
    Object? approvedAt = null,
    Object? depositBalance = null,
  }) {
    return _then(
      _value.copyWith(
            paymentId: null == paymentId
                ? _value.paymentId
                : paymentId // ignore: cast_nullable_to_non_nullable
                      as int,
            merchantOrderId: null == merchantOrderId
                ? _value.merchantOrderId
                : merchantOrderId // ignore: cast_nullable_to_non_nullable
                      as String,
            amount: null == amount
                ? _value.amount
                : amount // ignore: cast_nullable_to_non_nullable
                      as num,
            approvedStatus: null == approvedStatus
                ? _value.approvedStatus
                : approvedStatus // ignore: cast_nullable_to_non_nullable
                      as String,
            approvedAt: null == approvedAt
                ? _value.approvedAt
                : approvedAt // ignore: cast_nullable_to_non_nullable
                      as String,
            depositBalance: null == depositBalance
                ? _value.depositBalance
                : depositBalance // ignore: cast_nullable_to_non_nullable
                      as num,
          )
          as $Val,
    );
  }
}

/// @nodoc
abstract class _$$PaymentApproveImplCopyWith<$Res>
    implements $PaymentApproveCopyWith<$Res> {
  factory _$$PaymentApproveImplCopyWith(
    _$PaymentApproveImpl value,
    $Res Function(_$PaymentApproveImpl) then,
  ) = __$$PaymentApproveImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({
    int paymentId,
    String merchantOrderId,
    num amount,
    String approvedStatus,
    String approvedAt,
    num depositBalance,
  });
}

/// @nodoc
class __$$PaymentApproveImplCopyWithImpl<$Res>
    extends _$PaymentApproveCopyWithImpl<$Res, _$PaymentApproveImpl>
    implements _$$PaymentApproveImplCopyWith<$Res> {
  __$$PaymentApproveImplCopyWithImpl(
    _$PaymentApproveImpl _value,
    $Res Function(_$PaymentApproveImpl) _then,
  ) : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? paymentId = null,
    Object? merchantOrderId = null,
    Object? amount = null,
    Object? approvedStatus = null,
    Object? approvedAt = null,
    Object? depositBalance = null,
  }) {
    return _then(
      _$PaymentApproveImpl(
        paymentId: null == paymentId
            ? _value.paymentId
            : paymentId // ignore: cast_nullable_to_non_nullable
                  as int,
        merchantOrderId: null == merchantOrderId
            ? _value.merchantOrderId
            : merchantOrderId // ignore: cast_nullable_to_non_nullable
                  as String,
        amount: null == amount
            ? _value.amount
            : amount // ignore: cast_nullable_to_non_nullable
                  as num,
        approvedStatus: null == approvedStatus
            ? _value.approvedStatus
            : approvedStatus // ignore: cast_nullable_to_non_nullable
                  as String,
        approvedAt: null == approvedAt
            ? _value.approvedAt
            : approvedAt // ignore: cast_nullable_to_non_nullable
                  as String,
        depositBalance: null == depositBalance
            ? _value.depositBalance
            : depositBalance // ignore: cast_nullable_to_non_nullable
                  as num,
      ),
    );
  }
}

/// @nodoc
@JsonSerializable()
class _$PaymentApproveImpl implements _PaymentApprove {
  const _$PaymentApproveImpl({
    required this.paymentId,
    required this.merchantOrderId,
    required this.amount,
    required this.approvedStatus,
    required this.approvedAt,
    required this.depositBalance,
  });

  factory _$PaymentApproveImpl.fromJson(Map<String, dynamic> json) =>
      _$$PaymentApproveImplFromJson(json);

  @override
  final int paymentId;
  @override
  final String merchantOrderId;
  @override
  final num amount;
  @override
  final String approvedStatus;
  @override
  final String approvedAt;
  @override
  final num depositBalance;

  @override
  String toString() {
    return 'PaymentApprove(paymentId: $paymentId, merchantOrderId: $merchantOrderId, amount: $amount, approvedStatus: $approvedStatus, approvedAt: $approvedAt, depositBalance: $depositBalance)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PaymentApproveImpl &&
            (identical(other.paymentId, paymentId) ||
                other.paymentId == paymentId) &&
            (identical(other.merchantOrderId, merchantOrderId) ||
                other.merchantOrderId == merchantOrderId) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.approvedStatus, approvedStatus) ||
                other.approvedStatus == approvedStatus) &&
            (identical(other.approvedAt, approvedAt) ||
                other.approvedAt == approvedAt) &&
            (identical(other.depositBalance, depositBalance) ||
                other.depositBalance == depositBalance));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
    runtimeType,
    paymentId,
    merchantOrderId,
    amount,
    approvedStatus,
    approvedAt,
    depositBalance,
  );

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$PaymentApproveImplCopyWith<_$PaymentApproveImpl> get copyWith =>
      __$$PaymentApproveImplCopyWithImpl<_$PaymentApproveImpl>(
        this,
        _$identity,
      );

  @override
  Map<String, dynamic> toJson() {
    return _$$PaymentApproveImplToJson(this);
  }
}

abstract class _PaymentApprove implements PaymentApprove {
  const factory _PaymentApprove({
    required final int paymentId,
    required final String merchantOrderId,
    required final num amount,
    required final String approvedStatus,
    required final String approvedAt,
    required final num depositBalance,
  }) = _$PaymentApproveImpl;

  factory _PaymentApprove.fromJson(Map<String, dynamic> json) =
      _$PaymentApproveImpl.fromJson;

  @override
  int get paymentId;
  @override
  String get merchantOrderId;
  @override
  num get amount;
  @override
  String get approvedStatus;
  @override
  String get approvedAt;
  @override
  num get depositBalance;
  @override
  @JsonKey(ignore: true)
  _$$PaymentApproveImplCopyWith<_$PaymentApproveImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
