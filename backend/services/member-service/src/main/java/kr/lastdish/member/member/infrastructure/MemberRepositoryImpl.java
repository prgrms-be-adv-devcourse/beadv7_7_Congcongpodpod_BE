package kr.lastdish.member.member.infrastructure;

import java.util.Optional;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

  private final JpaMemberRepository jpaMemberRepository;

  @Override
  public Member save(Member member) {
    return jpaMemberRepository.save(member);
  }

  @Override
  public Optional<Member> findById(Long id) {
    // 👉 수정된 JpaMemberRepository 메서드 호출
    return jpaMemberRepository.findByIdAndIsDeletedFalse(id);
  }

  @Override
  public Optional<Member> findByUserName(String userName) {
    return jpaMemberRepository.findByUserNameAndIsDeletedFalse(userName);
  }

  @Override
  public Optional<Member> findByEmail(String email) {
    return jpaMemberRepository.findByEmailAndIsDeletedFalse(email);
  }

  @Override
  public boolean existsByUserName(String userName) {
    return jpaMemberRepository.existsByUserNameAndIsDeletedFalse(userName);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaMemberRepository.existsByEmailAndIsDeletedFalse(email);
  }
}
