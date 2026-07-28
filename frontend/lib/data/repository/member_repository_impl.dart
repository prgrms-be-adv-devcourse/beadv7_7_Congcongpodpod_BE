import 'package:dio/dio.dart';

import '../../core/network/api_error_mapper.dart';
import '../../domain/model/member.dart';
import '../../domain/repository/member_repository.dart';

/// MemberRepository 실제 구현. dio로 member-service(Member 컨트롤러, Auth 아님)를 호출한다.
///
/// ⚠️ member-service 안에서도 컨트롤러마다 에러 응답 모양이 다르다(`api-contracts.md` §0) —
/// `AuthController`는 아직 통일 안 된 포맷을 쓰지만, `MemberController`는 core-service와
/// 동일한 `{success,error:{code,message}}` 포맷을 쓴다. 그래서 auth_repository_impl.dart처럼
/// 따로 분기하지 않고 store/cart와 같은 [mapCoreServiceError]를 그대로 재사용한다.
class MemberRepositoryImpl implements MemberRepository {
  MemberRepositoryImpl({required Dio dio}) : _dio = dio;

  final Dio _dio;

  @override
  Future<Member> getMyInfo() async {
    try {
      final response = await _dio.get('/members/me');

      final body = response.data as Map<String, dynamic>;
      final data = body['data'] as Map<String, dynamic>;
      return Member.fromJson(data);
    } on DioException catch (e) {
      throw mapCoreServiceError(e);
    }
  }
}
