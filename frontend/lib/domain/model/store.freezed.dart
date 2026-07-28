// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'store.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Store _$StoreFromJson(Map<String, dynamic> json) {
  return _Store.fromJson(json);
}

/// @nodoc
mixin _$Store {
  int get storeId => throw _privateConstructorUsedError;
  String get storeName => throw _privateConstructorUsedError;
  String get storeAddress => throw _privateConstructorUsedError;
  String get storePhone =>
      throw _privateConstructorUsedError; // "HH:mm" 문자열 그대로 둔다 — 시간 계산이 필요해지면 그때 DateTime 등으로 파싱.
  String get openTime => throw _privateConstructorUsedError;
  String get closeTime => throw _privateConstructorUsedError;
  double get latitude => throw _privateConstructorUsedError;
  double get longitude =>
      throw _privateConstructorUsedError; // 서버가 안 내려주면 null일 수 있어 nullable + 기본 빈 리스트로 다룬다.
  List<String>? get holidays =>
      throw _privateConstructorUsedError; // StoreStatus: OPEN, CLOSED, STOPPED. enum 그대로 두지 않고 String으로
// 받아 화면에서 필요할 때 매핑 — Dish의 dishStatus도 문자열로 내려온다는 점과 일관되게.
  String get status =>
      throw _privateConstructorUsedError; // Category enum(15개, CHICKEN/KOREAN/... — category.dart의 categoryDisplayName 참고).
// ADR 017(백엔드)로 Dish가 아니라 Store 소유가 됨 — 그래서 여기 있다.
  String get category =>
      throw _privateConstructorUsedError; // 판매중(ON_SALE) 상품만 내려온다 — 서버가 필터링해서 줌(ADR 018).
// 매장에 지금 판매중인 상품이 없으면 빈 리스트로 온다(품절/마감 매장 등).
  List<Dish> get dishes => throw _privateConstructorUsedError;

  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;
  @JsonKey(ignore: true)
  $StoreCopyWith<Store> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StoreCopyWith<$Res> {
  factory $StoreCopyWith(Store value, $Res Function(Store) then) =
      _$StoreCopyWithImpl<$Res, Store>;
  @useResult
  $Res call(
      {int storeId,
      String storeName,
      String storeAddress,
      String storePhone,
      String openTime,
      String closeTime,
      double latitude,
      double longitude,
      List<String>? holidays,
      String status,
      String category,
      List<Dish> dishes});
}

/// @nodoc
class _$StoreCopyWithImpl<$Res, $Val extends Store>
    implements $StoreCopyWith<$Res> {
  _$StoreCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? storeId = null,
    Object? storeName = null,
    Object? storeAddress = null,
    Object? storePhone = null,
    Object? openTime = null,
    Object? closeTime = null,
    Object? latitude = null,
    Object? longitude = null,
    Object? holidays = freezed,
    Object? status = null,
    Object? category = null,
    Object? dishes = null,
  }) {
    return _then(_value.copyWith(
      storeId: null == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int,
      storeName: null == storeName
          ? _value.storeName
          : storeName // ignore: cast_nullable_to_non_nullable
              as String,
      storeAddress: null == storeAddress
          ? _value.storeAddress
          : storeAddress // ignore: cast_nullable_to_non_nullable
              as String,
      storePhone: null == storePhone
          ? _value.storePhone
          : storePhone // ignore: cast_nullable_to_non_nullable
              as String,
      openTime: null == openTime
          ? _value.openTime
          : openTime // ignore: cast_nullable_to_non_nullable
              as String,
      closeTime: null == closeTime
          ? _value.closeTime
          : closeTime // ignore: cast_nullable_to_non_nullable
              as String,
      latitude: null == latitude
          ? _value.latitude
          : latitude // ignore: cast_nullable_to_non_nullable
              as double,
      longitude: null == longitude
          ? _value.longitude
          : longitude // ignore: cast_nullable_to_non_nullable
              as double,
      holidays: freezed == holidays
          ? _value.holidays
          : holidays // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      category: null == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as String,
      dishes: null == dishes
          ? _value.dishes
          : dishes // ignore: cast_nullable_to_non_nullable
              as List<Dish>,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StoreImplCopyWith<$Res> implements $StoreCopyWith<$Res> {
  factory _$$StoreImplCopyWith(
          _$StoreImpl value, $Res Function(_$StoreImpl) then) =
      __$$StoreImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int storeId,
      String storeName,
      String storeAddress,
      String storePhone,
      String openTime,
      String closeTime,
      double latitude,
      double longitude,
      List<String>? holidays,
      String status,
      String category,
      List<Dish> dishes});
}

/// @nodoc
class __$$StoreImplCopyWithImpl<$Res>
    extends _$StoreCopyWithImpl<$Res, _$StoreImpl>
    implements _$$StoreImplCopyWith<$Res> {
  __$$StoreImplCopyWithImpl(
      _$StoreImpl _value, $Res Function(_$StoreImpl) _then)
      : super(_value, _then);

  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? storeId = null,
    Object? storeName = null,
    Object? storeAddress = null,
    Object? storePhone = null,
    Object? openTime = null,
    Object? closeTime = null,
    Object? latitude = null,
    Object? longitude = null,
    Object? holidays = freezed,
    Object? status = null,
    Object? category = null,
    Object? dishes = null,
  }) {
    return _then(_$StoreImpl(
      storeId: null == storeId
          ? _value.storeId
          : storeId // ignore: cast_nullable_to_non_nullable
              as int,
      storeName: null == storeName
          ? _value.storeName
          : storeName // ignore: cast_nullable_to_non_nullable
              as String,
      storeAddress: null == storeAddress
          ? _value.storeAddress
          : storeAddress // ignore: cast_nullable_to_non_nullable
              as String,
      storePhone: null == storePhone
          ? _value.storePhone
          : storePhone // ignore: cast_nullable_to_non_nullable
              as String,
      openTime: null == openTime
          ? _value.openTime
          : openTime // ignore: cast_nullable_to_non_nullable
              as String,
      closeTime: null == closeTime
          ? _value.closeTime
          : closeTime // ignore: cast_nullable_to_non_nullable
              as String,
      latitude: null == latitude
          ? _value.latitude
          : latitude // ignore: cast_nullable_to_non_nullable
              as double,
      longitude: null == longitude
          ? _value.longitude
          : longitude // ignore: cast_nullable_to_non_nullable
              as double,
      holidays: freezed == holidays
          ? _value._holidays
          : holidays // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as String,
      category: null == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as String,
      dishes: null == dishes
          ? _value._dishes
          : dishes // ignore: cast_nullable_to_non_nullable
              as List<Dish>,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StoreImpl implements _Store {
  const _$StoreImpl(
      {required this.storeId,
      required this.storeName,
      required this.storeAddress,
      required this.storePhone,
      required this.openTime,
      required this.closeTime,
      required this.latitude,
      required this.longitude,
      final List<String>? holidays,
      required this.status,
      required this.category,
      final List<Dish> dishes = const <Dish>[]})
      : _holidays = holidays,
        _dishes = dishes;

  factory _$StoreImpl.fromJson(Map<String, dynamic> json) =>
      _$$StoreImplFromJson(json);

  @override
  final int storeId;
  @override
  final String storeName;
  @override
  final String storeAddress;
  @override
  final String storePhone;
// "HH:mm" 문자열 그대로 둔다 — 시간 계산이 필요해지면 그때 DateTime 등으로 파싱.
  @override
  final String openTime;
  @override
  final String closeTime;
  @override
  final double latitude;
  @override
  final double longitude;
// 서버가 안 내려주면 null일 수 있어 nullable + 기본 빈 리스트로 다룬다.
  final List<String>? _holidays;
// 서버가 안 내려주면 null일 수 있어 nullable + 기본 빈 리스트로 다룬다.
  @override
  List<String>? get holidays {
    final value = _holidays;
    if (value == null) return null;
    if (_holidays is EqualUnmodifiableListView) return _holidays;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

// StoreStatus: OPEN, CLOSED, STOPPED. enum 그대로 두지 않고 String으로
// 받아 화면에서 필요할 때 매핑 — Dish의 dishStatus도 문자열로 내려온다는 점과 일관되게.
  @override
  final String status;
// Category enum(15개, CHICKEN/KOREAN/... — category.dart의 categoryDisplayName 참고).
// ADR 017(백엔드)로 Dish가 아니라 Store 소유가 됨 — 그래서 여기 있다.
  @override
  final String category;
// 판매중(ON_SALE) 상품만 내려온다 — 서버가 필터링해서 줌(ADR 018).
// 매장에 지금 판매중인 상품이 없으면 빈 리스트로 온다(품절/마감 매장 등).
  final List<Dish> _dishes;
// 판매중(ON_SALE) 상품만 내려온다 — 서버가 필터링해서 줌(ADR 018).
// 매장에 지금 판매중인 상품이 없으면 빈 리스트로 온다(품절/마감 매장 등).
  @override
  @JsonKey()
  List<Dish> get dishes {
    if (_dishes is EqualUnmodifiableListView) return _dishes;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_dishes);
  }

  @override
  String toString() {
    return 'Store(storeId: $storeId, storeName: $storeName, storeAddress: $storeAddress, storePhone: $storePhone, openTime: $openTime, closeTime: $closeTime, latitude: $latitude, longitude: $longitude, holidays: $holidays, status: $status, category: $category, dishes: $dishes)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StoreImpl &&
            (identical(other.storeId, storeId) || other.storeId == storeId) &&
            (identical(other.storeName, storeName) ||
                other.storeName == storeName) &&
            (identical(other.storeAddress, storeAddress) ||
                other.storeAddress == storeAddress) &&
            (identical(other.storePhone, storePhone) ||
                other.storePhone == storePhone) &&
            (identical(other.openTime, openTime) ||
                other.openTime == openTime) &&
            (identical(other.closeTime, closeTime) ||
                other.closeTime == closeTime) &&
            (identical(other.latitude, latitude) ||
                other.latitude == latitude) &&
            (identical(other.longitude, longitude) ||
                other.longitude == longitude) &&
            const DeepCollectionEquality().equals(other._holidays, _holidays) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.category, category) ||
                other.category == category) &&
            const DeepCollectionEquality().equals(other._dishes, _dishes));
  }

  @JsonKey(ignore: true)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      storeId,
      storeName,
      storeAddress,
      storePhone,
      openTime,
      closeTime,
      latitude,
      longitude,
      const DeepCollectionEquality().hash(_holidays),
      status,
      category,
      const DeepCollectionEquality().hash(_dishes));

  @JsonKey(ignore: true)
  @override
  @pragma('vm:prefer-inline')
  _$$StoreImplCopyWith<_$StoreImpl> get copyWith =>
      __$$StoreImplCopyWithImpl<_$StoreImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StoreImplToJson(
      this,
    );
  }
}

abstract class _Store implements Store {
  const factory _Store(
      {required final int storeId,
      required final String storeName,
      required final String storeAddress,
      required final String storePhone,
      required final String openTime,
      required final String closeTime,
      required final double latitude,
      required final double longitude,
      final List<String>? holidays,
      required final String status,
      required final String category,
      final List<Dish> dishes}) = _$StoreImpl;

  factory _Store.fromJson(Map<String, dynamic> json) = _$StoreImpl.fromJson;

  @override
  int get storeId;
  @override
  String get storeName;
  @override
  String get storeAddress;
  @override
  String get storePhone;
  @override // "HH:mm" 문자열 그대로 둔다 — 시간 계산이 필요해지면 그때 DateTime 등으로 파싱.
  String get openTime;
  @override
  String get closeTime;
  @override
  double get latitude;
  @override
  double get longitude;
  @override // 서버가 안 내려주면 null일 수 있어 nullable + 기본 빈 리스트로 다룬다.
  List<String>? get holidays;
  @override // StoreStatus: OPEN, CLOSED, STOPPED. enum 그대로 두지 않고 String으로
// 받아 화면에서 필요할 때 매핑 — Dish의 dishStatus도 문자열로 내려온다는 점과 일관되게.
  String get status;
  @override // Category enum(15개, CHICKEN/KOREAN/... — category.dart의 categoryDisplayName 참고).
// ADR 017(백엔드)로 Dish가 아니라 Store 소유가 됨 — 그래서 여기 있다.
  String get category;
  @override // 판매중(ON_SALE) 상품만 내려온다 — 서버가 필터링해서 줌(ADR 018).
// 매장에 지금 판매중인 상품이 없으면 빈 리스트로 온다(품절/마감 매장 등).
  List<Dish> get dishes;
  @override
  @JsonKey(ignore: true)
  _$$StoreImplCopyWith<_$StoreImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
