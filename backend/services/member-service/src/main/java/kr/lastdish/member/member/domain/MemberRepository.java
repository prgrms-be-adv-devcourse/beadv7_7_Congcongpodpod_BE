package kr.lastdish.member.member.domain;

import java.util.Optional;

public interface MemberRepository {
  Member save(Member member);

  Optional<Member> findById(Long id);

  Optional<Member> findWithLockById(Long id);

  // 탈퇴를 하지 않은 회원만 조회하는 메서드
  Optional<Member> findActiveById(Long id);

  Optional<Member> findActiveWithLockById(Long id);

  Optional<Member> findByUserName(String userName);

  Optional<Member> findByEmail(String email);

  Optional<Member> findByProviderAndProviderId(SocialProvider provider, String providerId);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
