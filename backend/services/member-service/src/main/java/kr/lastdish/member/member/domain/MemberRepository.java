package kr.lastdish.member.member.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByEmail(String email);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
