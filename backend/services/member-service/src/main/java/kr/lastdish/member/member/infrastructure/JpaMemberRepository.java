package kr.lastdish.member.member.infrastructure;

import java.util.Optional;
import kr.lastdish.member.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByUserName(String userName);

  Optional<Member> findByEmail(String email);

  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
