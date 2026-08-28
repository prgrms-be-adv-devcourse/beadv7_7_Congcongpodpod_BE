package kr.lastdish.ai.elastic.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import kr.lastdish.ai.elastic.domain.document.StoreDocument;
import kr.lastdish.ai.elastic.infrastructure.embedding.VectorSimilarityUtils;
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

  // 가중치
  private static final double WEIGHT_ES = 0.25;
  private static final double WEIGHT_DISTANCE = 0.10;
  private static final double WEIGHT_PRICE = 0.15;
  private static final double WEIGHT_DEADLINE = 0.10;
  private static final double WEIGHT_STORE_NAME_SIM = 0.10;
  private static final double WEIGHT_DISH_NAME_SIM = 0.15;
  private static final double WEIGHT_DESCRIPTION_SIM = 0.15;

  // 임계치
  private static final double MAXIMUM_DISTANCE_KM = 5.0;
  private static final double MAX_PRICE_KRW = 20000.0;
  private static final double MAX_URGENCY_WINDOW_MINUTES = 120.0;
  private static final double VECTOR_SIM_BADGE_THRESHOLD = 0.75;

  public List<StoreSearchResult> rankAndAssignBadges(
      List<SearchHit<StoreDocument>> searchHits,
      GeoPoint userLocation,
      boolean deadlineRequested,
      List<Float> queryVector,
      BigDecimal walletBalance) {

    if (searchHits == null || searchHits.isEmpty()) {
      return Collections.emptyList();
    }

    List<StoreSearchResultDtoHolder> holders =
        searchHits.stream()
            .map(hit -> calculateScore(hit, userLocation, queryVector))
            .sorted(
                Comparator.comparingDouble(StoreSearchResultDtoHolder::getTotalScore).reversed())
            .toList();

    assignBadges(holders, deadlineRequested, walletBalance);

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
      SearchHit<StoreDocument> hit, GeoPoint userLocation, List<Float> queryVector) {

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

    // 벡터 필드별 유사도, 쿼리 벡터 없으면 0으로 처리
    double rawStoreNameSim = 0.0;
    double rawDishNameSim = 0.0;
    double rawDescriptionSim = 0.0;

    if (queryVector != null && !queryVector.isEmpty()) {
      rawStoreNameSim =
          VectorSimilarityUtils.cosineSimilarity(queryVector, store.getStoreNameVector());
      rawDishNameSim =
          VectorSimilarityUtils.cosineSimilarity(queryVector, store.getDishNameVector());
      rawDescriptionSim =
          VectorSimilarityUtils.cosineSimilarity(queryVector, store.getDescriptionVector());
    }
    double normStoreNameSim = normalizeCosine(rawStoreNameSim);
    double normDishNameSim = normalizeCosine(rawDishNameSim);
    double normDescriptionSim = normalizeCosine(rawDescriptionSim);

    // 개인화 요소를 제외한 4가지 요소 기반 점수 산출
    double totalScore =
        (normEs * WEIGHT_ES)
            + (normDistance * WEIGHT_DISTANCE)
            + (normPrice * WEIGHT_PRICE)
            + (normDeadline * WEIGHT_DEADLINE)
            + (normStoreNameSim * WEIGHT_STORE_NAME_SIM)
            + (normDishNameSim * WEIGHT_DISH_NAME_SIM)
            + (normDescriptionSim * WEIGHT_DESCRIPTION_SIM);

    ScoreBreakdown breakdown =
        ScoreBreakdown.builder()
            .esScore(normEs)
            .distanceScore(normDistance)
            .rawDistanceKm(distanceKm)
            .deadlineScore(normDeadline)
            .priceScore(normPrice)
            .personalizationScore(0.0) // 개인화 미사용
            .storeNameSimScore(normStoreNameSim)
            .dishNameSimScore(normDishNameSim)
            .descriptionSimScore(normDescriptionSim)
            .build();

    return new StoreSearchResultDtoHolder(
        hit,
        totalScore,
        breakdown,
        distanceKm,
        minPrice,
        minutesUntilClose,
        rawStoreNameSim,
        rawDishNameSim,
        rawDescriptionSim);
  }

  private double normalizeCosine(double cosine) {
    return Math.max(0.0, Math.min(1.0, cosine));
  }

  private void assignBadges(
      List<StoreSearchResultDtoHolder> holders,
      boolean deadlineRequested,
      BigDecimal walletBalance) {
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
      if (holder.rawStoreNameSim >= VECTOR_SIM_BADGE_THRESHOLD) {
        holder.badges.add("가게명 일치도 높음");
      }
      if (holder.rawDishNameSim >= VECTOR_SIM_BADGE_THRESHOLD) {
        holder.badges.add("찾으시는 메뉴와 유사해요");
      }
      if (holder.rawDescriptionSim >= VECTOR_SIM_BADGE_THRESHOLD) {
        holder.badges.add("메뉴 설명과 잘 맞아요");
      }
      if (walletBalance != null
          && holder.minPrice < Double.MAX_VALUE
          && holder.minPrice <= walletBalance.doubleValue()) {
        holder.badges.add("예치금으로 구매 가능");
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

  /** 매장 내 판매 중인 메뉴들 중 실구매가 최저값을 반환, 메뉴가 없거나 가격 정보가 전혀 없으면 MAX_VALUE. */
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

  /** 판매중/재고>0인 메뉴 중 가장 이른 pickupEndTime까지 남은 분을 반환, 해당 메뉴가 없으면 MAX_VALUE. */
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
    private final double rawStoreNameSim;
    private final double rawDishNameSim;
    private final double rawDescriptionSim;
    private final List<String> badges = new ArrayList<>();

    public StoreSearchResultDtoHolder(
        SearchHit<StoreDocument> hit,
        double totalScore,
        ScoreBreakdown scoreBreakdown,
        double distanceKm,
        double minPrice,
        double minutesUntilClose,
        double rawStoreNameSim,
        double rawDishNameSim,
        double rawDescriptionSim) {
      this.hit = hit;
      this.totalScore = totalScore;
      this.scoreBreakdown = scoreBreakdown;
      this.distanceKm = distanceKm;
      this.minPrice = minPrice;
      this.minutesUntilClose = minutesUntilClose;
      this.rawStoreNameSim = rawStoreNameSim;
      this.rawDishNameSim = rawDishNameSim;
      this.rawDescriptionSim = rawDescriptionSim;
    }

    public double getTotalScore() {
      return totalScore;
    }
  }
}
