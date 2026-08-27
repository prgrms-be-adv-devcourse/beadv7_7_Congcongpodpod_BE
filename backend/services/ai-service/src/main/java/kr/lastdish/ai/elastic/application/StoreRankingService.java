package kr.lastdish.ai.elastic.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
  private static final double WEIGHT_ES = 0.40;
  private static final double WEIGHT_DISTANCE = 0.25;
  private static final double WEIGHT_PRICE = 0.25;
  private static final double WEIGHT_DEADLINE = 0.10;

  private static final double MAXIMUM_DISTANCE_KM = 5.0;

  // 최저가 정규화 상한선(원) - 이 가격 이상이면 가격 점수 0점. 실제 메뉴 가격 분포 보고 튜닝 필요.
  private static final double MAX_PRICE_KRW = 20000.0;
  // 2시간 이내만 임박도 점수 부여
  private static final double MAX_URGENCY_WINDOW_MINUTES = 120.0;

  public List<StoreSearchResult> rankAndAssignBadges(
      List<SearchHit<StoreDocument>> searchHits, GeoPoint userLocation, boolean deadlineRequested) {

    if (searchHits == null || searchHits.isEmpty()) {
      return Collections.emptyList();
    }

    List<StoreSearchResultDtoHolder> holders =
        searchHits.stream()
            .map(hit -> calculateScore(hit, userLocation))
            .sorted(
                Comparator.comparingDouble(StoreSearchResultDtoHolder::getTotalScore).reversed())
            .toList();

    assignBadges(holders, deadlineRequested);

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
      SearchHit<StoreDocument> hit, GeoPoint userLocation) {

    StoreDocument store = hit.getContent();
    double rawEsScore = hit.getScore();

    double distanceKm = calculateDistanceKm(userLocation, store.getLocation());
    double minPrice = extractMinPrice(store.getDishes());

    double normEs = Math.min(rawEsScore / 10.0, 1.0);
    double normDistance = Math.max(0.0, 1.0 - (distanceKm / MAXIMUM_DISTANCE_KM));
    double normPrice =
        minPrice == Double.MAX_VALUE ? 0.0 : Math.max(0.0, 1.0 - (minPrice / MAX_PRICE_KRW));
    double minutesUntilClose = extractMinutesUntilEarliestPickupEnd(store.getDishes());
    double normDeadline =
        minutesUntilClose == Double.MAX_VALUE
            ? 0.0
            : Math.max(0.0, 1.0 - (minutesUntilClose / MAX_URGENCY_WINDOW_MINUTES));

    // 개인화 요소를 제외한 4가지 요소 기반 점수 산출
    double totalScore =
        (normEs * WEIGHT_ES)
            + (normDistance * WEIGHT_DISTANCE)
            + (normPrice * WEIGHT_PRICE)
            + (normDeadline * WEIGHT_DEADLINE);

    ScoreBreakdown breakdown =
        ScoreBreakdown.builder()
            .esScore(normEs)
            .distanceScore(normDistance)
            .deadlineScore(normDeadline)
            .priceScore(normPrice)
            .personalizationScore(0.0) // 개인화 미사용 - 항상 0
            .build();
    return new StoreSearchResultDtoHolder(
        hit, totalScore, breakdown, distanceKm, minPrice, minutesUntilClose);
  }

  private void assignBadges(List<StoreSearchResultDtoHolder> holders, boolean deadlineRequested) {
    if (holders.isEmpty()) return;

    double minDistance =
        holders.stream().mapToDouble(h -> h.distanceKm).min().orElse(Double.MAX_VALUE);
    double minPriceOverall =
        holders.stream().mapToDouble(h -> h.minPrice).min().orElse(Double.MAX_VALUE);
    double minMinutesUntilCloseOverall =
        holders.stream().mapToDouble(h -> h.minutesUntilClose).min().orElse(Double.MAX_VALUE);

    for (StoreSearchResultDtoHolder holder : holders) {
      // 1. '가장 가까움' 배지
      if (holder.distanceKm <= minDistance && holder.distanceKm < MAXIMUM_DISTANCE_KM) {
        holder.badges.add("가장 가까움");
      }

      // 2. '최저가' 배지
      if (holder.minPrice <= minPriceOverall && holder.minPrice < Double.MAX_VALUE) {
        holder.badges.add("최저가");
      }
      // 3. 마감임박 배지
      if (deadlineRequested
          && holder.minutesUntilClose <= minMinutesUntilCloseOverall
          && holder.minutesUntilClose < Double.MAX_VALUE) {
        holder.badges.add("마감임박");
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

  /** 매장 내 판매 중인 메뉴들 중 실구매가(할인가 우선, 없으면 정가) 최저값을 반환한다. 메뉴가 없거나 가격 정보가 전혀 없으면 MAX_VALUE. */
  private double extractMinPrice(List<StoreDocument.DishItem> dishes) {
    if (dishes == null || dishes.isEmpty()) return Double.MAX_VALUE;

    return dishes.stream()
        .map(
            dish -> {
              BigDecimal price = dish.getDishPrice();
              BigDecimal discountPrice = dish.getDiscountPrice();
              BigDecimal effectivePrice = discountPrice != null ? discountPrice : price;
              return effectivePrice != null ? effectivePrice.doubleValue() : Double.MAX_VALUE;
            })
        .mapToDouble(Double::doubleValue)
        .min()
        .orElse(Double.MAX_VALUE);
  }

  /** 판매중/재고>0인 메뉴 중 가장 이른 pickupEndTime까지 남은 분(分)을 반환한다. 해당 메뉴가 없으면 MAX_VALUE. */
  private double extractMinutesUntilEarliestPickupEnd(List<StoreDocument.DishItem> dishes) {
    if (dishes == null || dishes.isEmpty()) return Double.MAX_VALUE;
    java.time.LocalTime now = java.time.LocalTime.now();

    return dishes.stream()
        .filter(d -> "ON_SALE".equals(d.getDishStatus()))
        .filter(d -> d.getStockQuantity() != null && d.getStockQuantity() > 0)
        .map(StoreDocument.DishItem::getPickupEndTime)
        .filter(java.util.Objects::nonNull)
        .mapToDouble(end -> java.time.Duration.between(now, end).toMinutes())
        .filter(minutes -> minutes >= 0)
        .min()
        .orElse(Double.MAX_VALUE);
  }

  private static class StoreSearchResultDtoHolder {
    private final SearchHit<StoreDocument> hit;
    private final double totalScore;
    private final ScoreBreakdown scoreBreakdown;
    private final double distanceKm;
    private final double minPrice;
    private final double minutesUntilClose;
    private final List<String> badges = new ArrayList<>();

    public StoreSearchResultDtoHolder(
        SearchHit<StoreDocument> hit,
        double totalScore,
        ScoreBreakdown scoreBreakdown,
        double distanceKm,
        double minPrice,
        double minutesUntilClose) {
      this.hit = hit;
      this.totalScore = totalScore;
      this.scoreBreakdown = scoreBreakdown;
      this.distanceKm = distanceKm;
      this.minPrice = minPrice;
      this.minutesUntilClose = minutesUntilClose;
    }

    public double getTotalScore() {
      return totalScore;
    }
  }
}
