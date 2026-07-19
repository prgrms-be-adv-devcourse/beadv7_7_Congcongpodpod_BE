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
    return jpaMemberRepository.findById(id);
  }

  @Override
  public Optional<Member> findByUserName(String userName) {
    return jpaMemberRepository.findByUserName(userName);
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
