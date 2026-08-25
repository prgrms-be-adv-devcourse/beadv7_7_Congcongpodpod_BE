package kr.lastdish.core.store.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "stores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Store {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "store_id")
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "store_name", nullable = false)
  private String storeName;

  @Column(name = "business_number", nullable = false)
  private String businessNumber;

  @Column(name = "store_address", nullable = false)
  private String storeAddress;

  @Column(name = "store_detail_address")
  private String storeDetailAddress;

  @Column(name = "store_phone", nullable = false)
  private String storePhone;

  @Column(name = "open_time", nullable = false)
  private LocalTime openTime;

  @Column(name = "close_time", nullable = false)
  private LocalTime closeTime;

  @Column(name = "next_closing_at")
  private LocalDateTime nextClosingAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private StoreStatus status;

  @Column(name = "latitude", nullable = false)
  private BigDecimal latitude;

  @Column(name = "longitude", nullable = false)
  private BigDecimal longitude;

  @Column(name = "updated_at")
  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Column(name = "event_version", nullable = false)
  private long eventVersion;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StoreHoliday> holidays = new ArrayList<>();

  public Store(
      Long memberId,
      String storeName,
      String businessNumber,
      String storeAddress,
      String storeDetailAddress,
      String storePhone,
      LocalTime openTime,
      LocalTime closeTime,
      BigDecimal latitude,
      BigDecimal longitude,
      Category category,
      LocalDateTime now) {
    this.memberId = memberId;
    this.storeName = storeName;
    this.businessNumber = businessNumber;
    this.storeAddress = storeAddress;
    this.storeDetailAddress = normalizeDetailAddress(storeDetailAddress);
    this.storePhone = storePhone;
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.nextClosingAt = calculateNextClosingAt(now);
    this.latitude = latitude;
    this.longitude = longitude;
    this.category = category;
    this.status = StoreStatus.OPEN;
    this.eventVersion = 0L;
    this.deleted = false;
  }

  public void addHoliday(DayOfWeek dayOfWeek) {
    holidays.add(new StoreHoliday(this, dayOfWeek));
  }

  public void update(
      String storeName,
      String storeAddress,
      String storeDetailAddress,
      String storePhone,
      LocalTime openTime,
      LocalTime closeTime,
      BigDecimal latitude,
      BigDecimal longitude,
      Category category) {
    this.storeName = storeName;
    this.storeAddress = storeAddress;
    this.storeDetailAddress = normalizeDetailAddress(storeDetailAddress);
    this.storePhone = storePhone;
    this.openTime = openTime;
    this.closeTime = closeTime;
    this.latitude = latitude;
    this.longitude = longitude;
    this.category = category;
  }

  // 기존 휴무일 제거 후 새로운 휴무일로 교체
  public void replaceHolidays(List<DayOfWeek> daysOfWeek) {
    holidays.clear();

    daysOfWeek.forEach(this::addHoliday);
  }

  /** 영업시간과 정기 휴무일을 기준으로 기준 시각 이후의 가장 가까운 마감 일시를 계산한다. */
  public void rescheduleNextClosingAt(LocalDateTime from) {
    this.nextClosingAt = calculateNextClosingAt(from);
  }

  public void validatePickupTime(LocalTime pickupStartTime, LocalTime pickupEndTime) {
    int businessDuration = forwardMinutes(openTime, closeTime);
    int pickupStartOffset = forwardMinutes(openTime, pickupStartTime);
    int pickupEndOffset = forwardMinutes(openTime, pickupEndTime);

    if (pickupEndOffset > businessDuration || pickupStartOffset > pickupEndOffset) {
      throw new BusinessException(ErrorCode.DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS);
    }
  }

  /**
   * 기준 시각에 이 매장이 주문을 받을 수 있는 상태인지 확인한다.
   *
   * <p>영업 상태 플래그만으로는 부족하다 — 플래그가 OPEN이어도 개점 전이거나 마감 후면 주문을 받을 수 없다. 영업시간을 함께 보지 않으면 개점 전 주문이 픽업 마감
   * 검증까지 흘러가 "픽업 마감 시간이 지났습니다"라는 엉뚱한 안내를 받는다.
   */
  public boolean isOpenAt(LocalDateTime now) {
    if (status != StoreStatus.OPEN) {
      return false;
    }

    return forwardMinutes(openTime, now.toLocalTime()) < forwardMinutes(openTime, closeTime);
  }

  private int forwardMinutes(LocalTime from, LocalTime to) {
    int minutes = to.toSecondOfDay() / 60 - from.toSecondOfDay() / 60;
    return Math.floorMod(minutes, 24 * 60);
  }

  private LocalDateTime calculateNextClosingAt(LocalDateTime from) {
    LocalDate firstBusinessDate = from.toLocalDate().minusDays(1);

    for (int dayOffset = 0; dayOffset < 8; dayOffset++) {
      LocalDate businessDate = firstBusinessDate.plusDays(dayOffset);
      if (isHoliday(businessDate.getDayOfWeek())) {
        continue;
      }

      LocalDateTime closingAt = businessDate.atTime(closeTime);
      if (!closeTime.isAfter(openTime)) {
        closingAt = closingAt.plusDays(1);
      }

      if (closingAt.isAfter(from)) {
        return closingAt;
      }
    }

    // 모든 요일이 휴무일인 매장은 자동 마감 대상이 없다.
    return null;
  }

  private boolean isHoliday(DayOfWeek dayOfWeek) {
    return holidays.stream().anyMatch(holiday -> holiday.getDayOfWeek() == dayOfWeek);
  }

  public boolean isOwnedBy(Long memberId) {
    return this.memberId.equals(memberId);
  }

  public void changeStatus(StoreStatus status) {
    this.status = status;
  }

  public void delete() {
    this.status = StoreStatus.STOPPED;
    this.holidays.clear();
    this.deleted = true;
  }

  public long nextEventVersion() {
    return ++this.eventVersion;
  }

  private String normalizeDetailAddress(String storeDetailAddress) {
    if (storeDetailAddress == null || storeDetailAddress.isBlank()) {
      return null;
    }

    return storeDetailAddress.trim();
  }
}
