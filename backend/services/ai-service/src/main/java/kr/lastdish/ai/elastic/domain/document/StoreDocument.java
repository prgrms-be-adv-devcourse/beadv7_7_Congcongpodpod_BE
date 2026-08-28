package kr.lastdish.ai.elastic.domain.document;

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

  @Field(type = FieldType.Date, format = DateFormat.hour_minute_second)
  private LocalTime openTime;

  @Field(type = FieldType.Date, format = DateFormat.hour_minute_second)
  private LocalTime closeTime;

  @Field(type = FieldType.Keyword)
  private String status;

  @GeoPointField private GeoPoint location;

  @Field(type = FieldType.Dense_Vector, dims = 1536, index = true, similarity = "cosine")
  private List<Float> storeNameVector;

  @Field(type = FieldType.Keyword)
  private String storeNameHash;

  @Field(type = FieldType.Dense_Vector, dims = 1536, index = true, similarity = "cosine")
  private List<Float> dishNameVector;

  @Field(type = FieldType.Keyword)
  private String dishNameHash;

  @Field(type = FieldType.Dense_Vector, dims = 1536, index = true, similarity = "cosine")
  private List<Float> descriptionVector;

  @Field(type = FieldType.Keyword)
  private String descriptionHash;

  private String category;

  // Nested 타입으로 지정하여 개별 메뉴 단위의 조건 검색 지원
  @Field(type = FieldType.Nested)
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

    @Field(type = FieldType.Keyword)
    private String category;

    private String thumbnailUrl;

    private Long stockQuantity;

    @Field(type = FieldType.Keyword)
    private String dishStatus;

    private BigDecimal dishPrice;

    private BigDecimal discountPrice;

    @Field(type = FieldType.Date, format = DateFormat.hour_minute_second)
    private LocalTime pickupStartTime;

    @Field(type = FieldType.Date, format = DateFormat.hour_minute_second)
    private LocalTime pickupEndTime;

    private boolean pickupSpansMidnight;
  }
}
