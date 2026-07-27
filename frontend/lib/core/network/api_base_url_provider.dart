import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'api_base_url_provider.g.dart';

@Riverpod(keepAlive: true)
String apiBaseUrl(Ref ref) {
  return const String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api/v1/',
  );
}
