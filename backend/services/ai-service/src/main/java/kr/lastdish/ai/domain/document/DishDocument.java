package kr.lastdish.ai.domain.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "dishes")
public class DishDocument {

  @Id private Long dishId;

  private Long storeId;

  @Field(type = FieldType.Text, analyzer = "nori")
  private String storeName;

  @Field(type = FieldType.Text, analyzer = "nori")
  private String dishName;

  @Field(type = FieldType.Text, analyzer = "nori")
  private String description;

  private String thumbnailUrl;

  private Long stockQuantity;

  private String dishStatus;

  private BigDecimal dishPrice;

  private BigDecimal discountPrice;

  @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
  private LocalDateTime registeredAt;

  @Field(type = FieldType.Long)
  private Long version;
}
