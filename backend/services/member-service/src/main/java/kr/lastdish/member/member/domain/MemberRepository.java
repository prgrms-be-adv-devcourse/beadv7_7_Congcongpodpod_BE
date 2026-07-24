package kr.lastdish.member.member.domain;

import java.util.Optional;

public interface MemberRepository {
  Member save(Member member);

  Optional<Member> findById(Long id);

  // 탈퇴를 하지 않은 회원만 조회하는 메서드
  Optional<Member> findActiveById(Long id);

  Optional<Member> findByUserName(String userName);

  Optional<Member> findByEmail(String email);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
