// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'deposit.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

DepositBalance _$DepositBalanceFromJson(Map<String, dynamic> json) {
  return _DepositBalance.fromJson(json);
}

/// @nodoc
mixin _$DepositBalance {
  int get memberId => throw _privateConstructorUsedError;
  num get balance => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $DepositBalanceCopyWith<DepositBalance> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DepositBalanceCopyWith<$Res> {
  factory $DepositBalanceCopyWith(
          DepositBalance value, $Res Function(DepositBalance) then) =
      _$DepositBalanceCopyWithImpl<$Res, DepositBalance>;
  @useResult
  $Res call({int memberId, num balance});
}

/// @nodoc
class _$DepositBalanceCopyWithImpl<$Res, $Val extends DepositBalance>
    implements $DepositBalanceCopyWith<$Res> {
  _$DepositBalanceCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? memberId = null,
    Object? balance = null,
  }) {
    return _then(_value.copyWith(
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      balance: null == balance
          ? _value.balance
          : balance // ignore: cast_nullable_to_non_nullable
              as num,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DepositBalanceImplCopyWith<$Res>
    implements $DepositBalanceCopyWith<$Res> {
  factory _$$DepositBalanceImplCopyWith(_$DepositBalanceImpl value,
          $Res Function(_$DepositBalanceImpl) then) =
      __$$DepositBalanceImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call({int memberId, num balance});
}

/// @nodoc
class __$$DepositBalanceImplCopyWithImpl<$Res>
    extends _$DepositBalanceCopyWithImpl<$Res, _$DepositBalanceImpl>
    implements _$$DepositBalanceImplCopyWith<$Res> {
  __$$DepositBalanceImplCopyWithImpl(
      _$DepositBalanceImpl _value, $Res Function(_$DepositBalanceImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? memberId = null,
    Object? balance = null,
  }) {
    return _then(_$DepositBalanceImpl(
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      balance: null == balance
          ? _value.balance
          : balance // ignore: cast_nullable_to_non_nullable
              as num,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DepositBalanceImpl implements _DepositBalance {
  const _$DepositBalanceImpl({required this.memberId, required this.balance});

  factory _$DepositBalanceImpl.fromJson(Map<String, dynamic> json) =>
      _$$DepositBalanceImplFromJson(json);

  @override
  final int memberId;
  @override
  final num balance;

  @override
  String toString() {
    return 'DepositBalance(memberId: $memberId, balance: $balance)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DepositBalanceImpl &&
            (identical(other.memberId, memberId) ||
                other.memberId == memberId) &&
            (identical(other.balance, balance) || other.balance == balance));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, memberId, balance);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$DepositBalanceImplCopyWith<_$DepositBalanceImpl> get copyWith =>
      __$$DepositBalanceImplCopyWithImpl<_$DepositBalanceImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DepositBalanceImplToJson(
      this,
    );
  }
}

abstract class _DepositBalance implements DepositBalance {
  const factory _DepositBalance(
      {required final int memberId,
      required final num balance}) = _$DepositBalanceImpl;

  factory _DepositBalance.fromJson(Map<String, dynamic> json) =
      _$DepositBalanceImpl.fromJson;

  @override
  int get memberId;
  @override
  num get balance;
  @override
  @JsonKey(ignore: true)
  _$$DepositBalanceImplCopyWith<_$DepositBalanceImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

DepositHistoryEntry _$DepositHistoryEntryFromJson(Map<String, dynamic> json) {
  return _DepositHistoryEntry.fromJson(json);
}

/// @nodoc
mixin _$DepositHistoryEntry {
  int get id => throw _privateConstructorUsedError;
  int? get orderId => throw _privateConstructorUsedError;
  int? get paymentId => throw _privateConstructorUsedError;
  String get type => throw _privateConstructorUsedError;
  num get amount => throw _privateConstructorUsedError;
  num get balanceAfter => throw _privateConstructorUsedError;
  String get createdAt => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $DepositHistoryEntryCopyWith<DepositHistoryEntry> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DepositHistoryEntryCopyWith<$Res> {
  factory $DepositHistoryEntryCopyWith(
          DepositHistoryEntry value, $Res Function(DepositHistoryEntry) then) =
      _$DepositHistoryEntryCopyWithImpl<$Res, DepositHistoryEntry>;
  @useResult
  $Res call(
      {int id,
      int? orderId,
      int? paymentId,
      String type,
      num amount,
      num balanceAfter,
      String createdAt});
}

/// @nodoc
class _$DepositHistoryEntryCopyWithImpl<$Res, $Val extends DepositHistoryEntry>
    implements $DepositHistoryEntryCopyWith<$Res> {
  _$DepositHistoryEntryCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? orderId = freezed,
    Object? paymentId = freezed,
    Object? type = null,
    Object? amount = null,
    Object? balanceAfter = null,
    Object? createdAt = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      orderId: freezed == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int?,
      paymentId: freezed == paymentId
          ? _value.paymentId
          : paymentId // ignore: cast_nullable_to_non_nullable
              as int?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as num,
      balanceAfter: null == balanceAfter
          ? _value.balanceAfter
          : balanceAfter // ignore: cast_nullable_to_non_nullable
              as num,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as String,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DepositHistoryEntryImplCopyWith<$Res>
    implements $DepositHistoryEntryCopyWith<$Res> {
  factory _$$DepositHistoryEntryImplCopyWith(_$DepositHistoryEntryImpl value,
          $Res Function(_$DepositHistoryEntryImpl) then) =
      __$$DepositHistoryEntryImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int? orderId,
      int? paymentId,
      String type,
      num amount,
      num balanceAfter,
      String createdAt});
}

/// @nodoc
class __$$DepositHistoryEntryImplCopyWithImpl<$Res>
    extends _$DepositHistoryEntryCopyWithImpl<$Res, _$DepositHistoryEntryImpl>
    implements _$$DepositHistoryEntryImplCopyWith<$Res> {
  __$$DepositHistoryEntryImplCopyWithImpl(_$DepositHistoryEntryImpl _value,
      $Res Function(_$DepositHistoryEntryImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? orderId = freezed,
    Object? paymentId = freezed,
    Object? type = null,
    Object? amount = null,
    Object? balanceAfter = null,
    Object? createdAt = null,
  }) {
    return _then(_$DepositHistoryEntryImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      orderId: freezed == orderId
          ? _value.orderId
          : orderId // ignore: cast_nullable_to_non_nullable
              as int?,
      paymentId: freezed == paymentId
          ? _value.paymentId
          : paymentId // ignore: cast_nullable_to_non_nullable
              as int?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as num,
      balanceAfter: null == balanceAfter
          ? _value.balanceAfter
          : balanceAfter // ignore: cast_nullable_to_non_nullable
              as num,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as String,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DepositHistoryEntryImpl implements _DepositHistoryEntry {
  const _$DepositHistoryEntryImpl(
      {required this.id,
      this.orderId,
      this.paymentId,
      required this.type,
      required this.amount,
      required this.balanceAfter,
      required this.createdAt});

  factory _$DepositHistoryEntryImpl.fromJson(Map<String, dynamic> json) =>
      _$$DepositHistoryEntryImplFromJson(json);

  @override
  final int id;
  @override
  final int? orderId;
  @override
  final int? paymentId;
  @override
  final String type;
  @override
  final num amount;
  @override
  final num balanceAfter;
  @override
  final String createdAt;

  @override
  String toString() {
    return 'DepositHistoryEntry(id: $id, orderId: $orderId, paymentId: $paymentId, type: $type, amount: $amount, balanceAfter: $balanceAfter, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DepositHistoryEntryImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.orderId, orderId) || other.orderId == orderId) &&
            (identical(other.paymentId, paymentId) ||
                other.paymentId == paymentId) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.balanceAfter, balanceAfter) ||
                other.balanceAfter == balanceAfter) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(runtimeType, id, orderId, paymentId, type,
      amount, balanceAfter, createdAt);

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$DepositHistoryEntryImplCopyWith<_$DepositHistoryEntryImpl> get copyWith =>
      __$$DepositHistoryEntryImplCopyWithImpl<_$DepositHistoryEntryImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DepositHistoryEntryImplToJson(
      this,
    );
  }
}

abstract class _DepositHistoryEntry implements DepositHistoryEntry {
  const factory _DepositHistoryEntry(
      {required final int id,
      final int? orderId,
      final int? paymentId,
      required final String type,
      required final num amount,
      required final num balanceAfter,
      required final String createdAt}) = _$DepositHistoryEntryImpl;

  factory _DepositHistoryEntry.fromJson(Map<String, dynamic> json) =
      _$DepositHistoryEntryImpl.fromJson;

  @override
  int get id;
  @override
  int? get orderId;
  @override
  int? get paymentId;
  @override
  String get type;
  @override
  num get amount;
  @override
  num get balanceAfter;
  @override
  String get createdAt;
  @override
  @JsonKey(ignore: true)
  _$$DepositHistoryEntryImplCopyWith<_$DepositHistoryEntryImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
