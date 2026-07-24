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

  // 탈퇴 여부 상관없이 조회
  @Override
  public Optional<Member> findById(Long id) {
    return jpaMemberRepository.findById(id);
  }

  // 일반 조회 시 탈퇴된 회원을 제외하고 싶을 때 사용
  @Override
  public Optional<Member> findActiveById(Long id) {
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
    return jpaMemberRepository.existsByUserName(userName);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaMemberRepository.existsByEmail(email);
  }
}
