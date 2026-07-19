package kr.lastdish.core.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Getter
@Entity
@Table(name = "store_holidays")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    StoreHoliday(
            Store store,
            DayOfWeek dayOfWeek
    ) {
        this.store = store;
        this.dayOfWeek = dayOfWeek;
    }
}