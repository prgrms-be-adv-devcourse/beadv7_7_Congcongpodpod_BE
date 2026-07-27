import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show debugPrint;

/// 로컬 개발용 콘솔 로그. dio 기본 `LogInterceptor`는 요청/응답마다 헤더까지
/// 여러 줄로 늘어놓아서 콘솔이 금방 지저분해진다 — 대신 요청/응답/에러를
/// 한 줄씩만 찍는다:
/// ```
/// → POST /auth/login {"email":"a@a.com","password":"***"}
/// ← 200 POST /auth/login 42ms {"success":true,"data":{...}}
/// ✕ 400 POST /auth/signup 55ms {"success":false,"error":{"code":"M006","message":"이미 사용 중인 아이디입니다."}}
/// ```
/// `password` 같은 민감한 필드는 마스킹하고, JWT처럼 긴 문자열/전체 바디는 잘라서
/// 한 화면에 최대한 많은 요청이 보이게 한다. dio_provider.dart에서 디버그 빌드에만 붙인다.
class CompactApiLoggingInterceptor extends Interceptor {
  static const _startTimeKey = '__requestStartedAt';
  static const _maxBodyLength = 300;
  static const _maxStringValueLength = 80;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    options.extra[_startTimeKey] = DateTime.now();
    debugPrint(
      '→ ${options.method} ${options.uri.path}${_queryOf(options)} '
      '${_format(options.data)}',
    );
    handler.next(options);
  }

  @override
  void onResponse(Response<dynamic> response, ResponseInterceptorHandler handler) {
    final options = response.requestOptions;
    debugPrint(
      '← ${response.statusCode} ${options.method} ${options.uri.path} '
      '${_elapsedMs(options)}ms ${_format(response.data)}',
    );
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final options = err.requestOptions;
    debugPrint(
      '✕ ${err.response?.statusCode ?? '-'} ${options.method} ${options.uri.path} '
      '${_elapsedMs(options)}ms ${_format(err.response?.data ?? err.message)}',
    );
    handler.next(err);
  }

  String _queryOf(RequestOptions options) =>
      options.uri.query.isEmpty ? '' : '?${options.uri.query}';

  int _elapsedMs(RequestOptions options) {
    final start = options.extra[_startTimeKey];
    return start is DateTime ? DateTime.now().difference(start).inMilliseconds : -1;
  }

  String _format(Object? data) {
    if (data == null) return '';
    String encoded;
    try {
      encoded = jsonEncode(_redact(data));
    } on JsonUnsupportedObjectError {
      encoded = data.toString();
    }
    return encoded.length > _maxBodyLength
        ? '${encoded.substring(0, _maxBodyLength)}…'
        : encoded;
  }

  /// 비밀번호는 값을 통째로 가리고, 그 외 긴 문자열(JWT 등)은 앞부분만 남기고 자른다.
  Object? _redact(Object? value) {
    if (value is Map) {
      return value.map((key, entryValue) {
        final isSensitive = key is String && key.toLowerCase().contains('password');
        return MapEntry(key, isSensitive ? '***' : _redact(entryValue));
      });
    }
    if (value is List) return value.map(_redact).toList();
    if (value is String && value.length > _maxStringValueLength) {
      return '${value.substring(0, _maxStringValueLength)}…';
    }
    return value;
  }
}
