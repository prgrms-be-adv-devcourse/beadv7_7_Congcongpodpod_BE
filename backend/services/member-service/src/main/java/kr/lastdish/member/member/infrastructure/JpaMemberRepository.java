package kr.lastdish.member.member.infrastructure;

import java.util.Optional;
import kr.lastdish.member.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByIdAndIsDeletedFalse(Long id);

  Optional<Member> findByUserNameAndIsDeletedFalse(String userName);

  Optional<Member> findByEmailAndIsDeletedFalse(String email);

  boolean existsByUserNameAndIsDeletedFalse(String userName);

  boolean existsByEmailAndIsDeletedFalse(String email);
}
