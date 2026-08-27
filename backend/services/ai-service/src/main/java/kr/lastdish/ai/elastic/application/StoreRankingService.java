package kr.lastdish.ai.elastic.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.presentation.dto.StoreResponse;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchResult;
import kr.lastdish.ai.elastic.presentation.dto.StoreSearchResult.ScoreBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreRankingService {

  // 가중치 고정 상수
  private static final double WEIGHT_ES = 0.45;
  private static final double WEIGHT_DISTANCE = 0.20;
  private static final double WEIGHT_DISCOUNT = 0.10;
  private static final double WEIGHT_PERSONALIZATION = 0.15;

  private static final double MAXIMUM_DISTANCE_KM = 5.0;
  private static final double MEANING_SEARCH_ES_THRESHOLD = 1.0;

  public List<StoreSearchResult> rankAndAssignBadges(
      List<SearchHit<StoreDocument>> searchHits,
      GeoPoint userLocation,
      Set<Long> completedPickupStoreIds) {

    if (searchHits == null || searchHits.isEmpty()) {
      return Collections.emptyList();
    }

    List<StoreSearchResultDtoHolder> holders =
        searchHits.stream()
            .map(hit -> calculateScore(hit, userLocation, completedPickupStoreIds))
            .sorted(
                Comparator.comparingDouble(StoreSearchResultDtoHolder::getTotalScore).reversed())
            .toList();

    assignBadges(holders);

    return holders.stream()
        .map(
            holder ->
                StoreSearchResult.builder()
                    .store(StoreResponse.from(holder.hit.getContent()))
                    .totalScore(holder.totalScore)
                    .scoreBreakdown(holder.scoreBreakdown)
                    .badges(holder.badges)
                    .build())
        .toList();
  }

  private StoreSearchResultDtoHolder calculateScore(
      SearchHit<StoreDocument> hit, GeoPoint userLocation, Set<Long> completedPickupStoreIds) {

    StoreDocument store = hit.getContent();
    double rawEsScore = hit.getScore();

    double distanceKm = calculateDistanceKm(userLocation, store.getLocation());
    double maxDiscountRate = extractMaxDiscountRate(store.getDishes());

    double normEs = Math.min(rawEsScore / 10.0, 1.0);
    double normDistance = Math.max(0.0, 1.0 - (distanceKm / MAXIMUM_DISTANCE_KM));
    double normDiscount = maxDiscountRate;

    boolean hasPickupHistory =
        completedPickupStoreIds != null && completedPickupStoreIds.contains(store.getStoreId());
    double normPersonalization = hasPickupHistory ? 1.0 : 0.0;

    // 마감시간 요소를 제외한 4가지 요소 기반 점수 산출
    double totalScore =
        (normEs * WEIGHT_ES)
            + (normDistance * WEIGHT_DISTANCE)
            + (normDiscount * WEIGHT_DISCOUNT)
            + (normPersonalization * WEIGHT_PERSONALIZATION);

    ScoreBreakdown breakdown =
        ScoreBreakdown.builder()
            .esScore(normEs)
            .distanceScore(normDistance)
            .deadlineScore(0.0) // 마감시간 점수 제거
            .discountRateScore(normDiscount)
            .personalizationScore(normPersonalization)
            .build();

    return new StoreSearchResultDtoHolder(
        hit, totalScore, breakdown, distanceKm, maxDiscountRate, hasPickupHistory, rawEsScore);
  }

  private void assignBadges(List<StoreSearchResultDtoHolder> holders) {
    if (holders.isEmpty()) return;

    double minDistance =
        holders.stream().mapToDouble(h -> h.distanceKm).min().orElse(Double.MAX_VALUE);
    double maxDiscount = holders.stream().mapToDouble(h -> h.maxDiscountRate).max().orElse(0.0);

    for (StoreSearchResultDtoHolder holder : holders) {
      // 1. '가장 가까움' 배지
      if (holder.distanceKm <= minDistance && holder.distanceKm < MAXIMUM_DISTANCE_KM) {
        holder.badges.add("가장 가까움");
      }

      // 2. '할인율 최고' 배지
      if (holder.maxDiscountRate >= maxDiscount && holder.maxDiscountRate > 0.0) {
        holder.badges.add("할인율 최고");
      }

      // 3. '취향 맞음' 배지
      if (holder.hasPickupHistory) {
        holder.badges.add("취향 맞음");
      }

      // 4. '뜻으로 찾음' 배지
      if (holder.rawEsScore < MEANING_SEARCH_ES_THRESHOLD
          && holder.scoreBreakdown.getEsScore() > 0.3) {
        holder.badges.add("뜻으로 찾음");
      }
    }
  }

  private double calculateDistanceKm(GeoPoint p1, GeoPoint p2) {
    if (p1 == null || p2 == null) return MAXIMUM_DISTANCE_KM;
    double lat1 = Math.toRadians(p1.getLat());
    double lon1 = Math.toRadians(p1.getLon());
    double lat2 = Math.toRadians(p2.getLat());
    double lon2 = Math.toRadians(p2.getLon());

    double dLat = lat2 - lat1;
    double dLon = lon2 - lon1;

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return 6371.0 * c;
  }

  private double extractMaxDiscountRate(List<StoreDocument.DishItem> dishes) {
    if (dishes == null || dishes.isEmpty()) return 0.0;

    return dishes.stream()
        .map(
            dish -> {
              BigDecimal price = dish.getDishPrice();
              BigDecimal discountPrice = dish.getDiscountPrice();
              if (price == null || discountPrice == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                return 0.0;
              }
              BigDecimal discount = price.subtract(discountPrice);
              return discount.divide(price, 4, RoundingMode.HALF_UP).doubleValue();
            })
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0.0);
  }

  private static class StoreSearchResultDtoHolder {
    private final SearchHit<StoreDocument> hit;
    private final double totalScore;
    private final ScoreBreakdown scoreBreakdown;
    private final double distanceKm;
    private final double maxDiscountRate;
    private final boolean hasPickupHistory;
    private final double rawEsScore;
    private final List<String> badges = new ArrayList<>();

    public StoreSearchResultDtoHolder(
        SearchHit<StoreDocument> hit,
        double totalScore,
        ScoreBreakdown scoreBreakdown,
        double distanceKm,
        double maxDiscountRate,
        boolean hasPickupHistory,
        double rawEsScore) {
      this.hit = hit;
      this.totalScore = totalScore;
      this.scoreBreakdown = scoreBreakdown;
      this.distanceKm = distanceKm;
      this.maxDiscountRate = maxDiscountRate;
      this.hasPickupHistory = hasPickupHistory;
      this.rawEsScore = rawEsScore;
    }

    public double getTotalScore() {
      return totalScore;
    }
  }
}
