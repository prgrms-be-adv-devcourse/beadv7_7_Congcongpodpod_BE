package kr.lastdish.core.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "stores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "store_phone", nullable = false)
    private String storePhone;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoreStatus status;

    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @OneToMany(
            mappedBy = "store",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StoreHoliday> holidays = new ArrayList<>();

    public Store(
            Long memberId,
            String storeName,
            String businessNumber,
            String storeAddress,
            String storePhone,
            LocalTime openTime,
            LocalTime closeTime,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.memberId = memberId;
        this.storeName = storeName;
        this.businessNumber = businessNumber;
        this.storeAddress = storeAddress;
        this.storePhone = storePhone;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = StoreStatus.OPEN;
        this.deleted = false;
    }

    public void addHoliday(DayOfWeek dayOfWeek) {
        holidays.add(
                new StoreHoliday(this, dayOfWeek)
        );
    }

    public void update(
            String storeName,
            String storeAddress,
            String storePhone,
            LocalTime openTime,
            LocalTime closeTime,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.storeName = storeName;
        this.storeAddress = storeAddress;
        this.storePhone = storePhone;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 기존 휴무일 제거 후 새로운 휴무일로 교체
    public void replaceHolidays(
            List<DayOfWeek> daysOfWeek
    ) {
        holidays.clear();

        daysOfWeek.forEach(this::addHoliday);
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void changeStatus(StoreStatus status) {
        this.status = status;
    }
}
