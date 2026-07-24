package kr.lastdish.member.member.infrastructure;

import java.util.Optional;
import kr.lastdish.member.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByIdAndIsDeletedFalse(Long id);

  Optional<Member> findByUserNameAndIsDeletedFalse(String userName);

  Optional<Member> findByEmailAndIsDeletedFalse(String email);

  // 검사를 할 때 탈퇴한 회원이 쓰던 아이디와 이메일도 포함해서 체크
  boolean existsByUserName(String userName);

  boolean existsByEmail(String email);
}
