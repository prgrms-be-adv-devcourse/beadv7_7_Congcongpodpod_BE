package kr.lastdish.ai.domain.document;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "stores")
public class StoreDocument {

  @Id private Long storeId;

  @Field(type = FieldType.Text, analyzer = "nori")
  private String storeName;

  @Field(type = FieldType.Text, analyzer = "nori")
  private String storeAddress;

  private LocalTime openTime;

  private LocalTime closeTime;

  private String status;

  @GeoPointField private GeoPoint location;

  private String category;

  private List<DishItem> dishes;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DishItem {
    private Long dishId;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String dishName;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    private String category;

    private String thumbnailUrl;

    private Long stockQuantity;

    private String dishStatus;

    private BigDecimal dishPrice;

    private BigDecimal discountPrice;

    private LocalTime pickupStartTime;

    private LocalTime pickupEndTime;
  }
}
