package kr.lastdish.core.level.application;

import java.math.BigDecimal;
import kr.lastdish.core.level.application.dto.LevelResponse;
import kr.lastdish.core.level.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevelService {
  private final LevelRepository levelRepository;
  private final LevelHistoryRepository levelHistoryRepository;

  // 회원의 Level 조회 시점에 정보가 생성되지 않은 회원인 경우, 기본 등급(LEVEL_1)으로 신규 생성하여 반환
  @Transactional(readOnly = true)
  public Level getOrDefaultLevel(Long memberId) {
    return levelRepository.findByMemberId(memberId).orElseGet(() -> Level.createDefault(memberId));
  }

  @Transactional(readOnly = true)
  public LevelResponse getLevel(Long memberId) {
    return LevelResponse.from(getOrDefaultLevel(memberId));
  }

  // 픽업완료 시 구매 횟수 증가, 등급 재계산 및 승급 처리, 승급 시 이력 기록
  @Transactional
  public void recordPurchase(Long memberId, BigDecimal discountAmount) {

    levelRepository.createDefaultIfAbsent(memberId);

    Level level =
        levelRepository
            .findWithLockByMemberId(memberId)
            .orElseThrow(() -> new IllegalStateException("Level 생성 후 조회에 실패했습니다."));

    level.addPurchase();
    level.addDiscountAmount(discountAmount);

    DishLevel oldLevel = level.getDishLevel();
    boolean upgraded = level.upgradeLevel();

    if (upgraded) {
      levelHistoryRepository.save(
          LevelHistory.recordUpgrade(
              memberId, oldLevel, level.getDishLevel(), level.getPurchaseCount()));
    }
  }

  // 회원의 현재 등급에 해당하는 적립률 조회 (Point 도메인이 적립 계산 시 사용)
  @Transactional(readOnly = true)
  public BigDecimal getPointEarningRate(Long memberId) {
    return getOrDefaultLevel(memberId).getDishLevel().getPointPercent();
  }
}
