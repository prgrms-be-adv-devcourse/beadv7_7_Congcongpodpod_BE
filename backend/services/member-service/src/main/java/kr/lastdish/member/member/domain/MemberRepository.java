package kr.lastdish.member.member.domain;

import java.util.Optional;

public interface MemberRepository {
  Member save(Member member);

  Optional<Member> findById(Long id);

  Optional<Member> findByUserName(String userName);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
