package kr.lastdish.core.cart.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.core.cart.presentation.dto.CartItemAddRequest;
import kr.lastdish.core.cart.presentation.dto.CartItemUpdateRequest;
import kr.lastdish.core.dish.domain.Category;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cart의 생성~조회~상품 추가/수정/삭제~장바구니 삭제까지 이어지는 happy path 통합 테스트. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DishRepository dishRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void 장바구니_생성부터_상품_담기_수정_삭제까지_전체_흐름이_동작한다() throws Exception {
    Dish dish =
        dishRepository.save(
            Dish.create(
                1L,
                "치킨마요 마감할인 세트",
                LocalDateTime.now(),
                "마감 임박 할인 상품",
                Category.KOREAN,
                null,
                10L,
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(5000)));
    Long memberId = 100L;

    // 1) 장바구니 조회 -> 없으면 생성됨 (별도 생성 API 없음)
    String getOrCreateResponse =
        mockMvc
            .perform(get("/api/v1/carts/members/{memberId}", memberId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberId").value(memberId))
            .andExpect(jsonPath("$.data.items").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long cartId = objectMapper.readTree(getOrCreateResponse).path("data").path("cartId").asLong();

    // 2) 상품 추가 (수량 미전달 -> 기본값 1)
    String addItemResponse =
        mockMvc
            .perform(
                post("/api/v1/carts/{cartId}/items", cartId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CartItemAddRequest(dish.getId(), null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.dishName").value("치킨마요 마감할인 세트"))
            .andExpect(jsonPath("$.data.unitPrice").value(5000))
            .andExpect(jsonPath("$.data.quantity").value(1))
            .andExpect(jsonPath("$.data.subtotalPrice").value(5000))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long itemId = objectMapper.readTree(addItemResponse).path("data").path("cartItemId").asLong();

    // 3) 장바구니 조회 -> 담긴 상품과 합계 확인
    mockMvc
        .perform(get("/api/v1/carts/members/{memberId}", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].cartItemId").value(itemId))
        .andExpect(jsonPath("$.data.items[0].status").value("AVAILABLE"))
        .andExpect(jsonPath("$.data.items[0].orderable").value(true))
        .andExpect(jsonPath("$.data.totalPrice").value(5000));

    // 4) 수량 변경
    mockMvc
        .perform(
            patch("/api/v1/carts/{cartId}/items/{itemId}", cartId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(3L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.quantity").value(3))
        .andExpect(jsonPath("$.data.subtotalPrice").value(15000));

    // 5) 상품 삭제
    mockMvc
        .perform(delete("/api/v1/carts/{cartId}/items/{itemId}", cartId, itemId))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/carts/members/{memberId}", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isEmpty());

    // 6) 상품을 다시 담은 뒤 장바구니 비우기 -> Cart 자체는 남고 상품만 사라진다
    mockMvc
        .perform(
            post("/api/v1/carts/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(dish.getId(), 2L))))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/v1/carts/{cartId}", cartId)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/carts/members/{memberId}", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isEmpty());
  }

  @Test
  void 다른_상품으로_교체하면_dishId도_함께_바뀐다() throws Exception {
    Dish dishA =
        dishRepository.save(
            Dish.create(
                1L,
                "치킨마요 마감할인 세트",
                LocalDateTime.now(),
                "마감 임박 할인 상품",
                Category.KOREAN,
                null,
                10L,
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(5000)));
    Dish dishB =
        dishRepository.save(
            Dish.create(
                1L,
                "소불고기 마감할인 세트",
                LocalDateTime.now(),
                "마감 임박 할인 상품",
                Category.KOREAN,
                null,
                10L,
                BigDecimal.valueOf(9000),
                BigDecimal.valueOf(6000)));
    Long memberId = 200L;

    String getOrCreateResponse =
        mockMvc
            .perform(get("/api/v1/carts/members/{memberId}", memberId)) // 조회로 Cart 생성
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long cartId = objectMapper.readTree(getOrCreateResponse).path("data").path("cartId").asLong();

    mockMvc
        .perform(
            post("/api/v1/carts/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new CartItemAddRequest(dishA.getId(), 1L))))
        .andExpect(status().isOk());

    // 상품 A -> B로 교체
    mockMvc
        .perform(
            post("/api/v1/carts/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new CartItemAddRequest(dishB.getId(), 1L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.dishId").value(dishB.getId()))
        .andExpect(jsonPath("$.data.dishName").value("소불고기 마감할인 세트"));

    // DB에서 다시 읽어와도(영속성 컨텍스트 캐시가 아니라 실제 저장 결과) dishId가 B여야 한다
    mockMvc
        .perform(get("/api/v1/carts/members/{memberId}", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].dishId").value(dishB.getId()))
        .andExpect(jsonPath("$.data.items[0].dishName").value("소불고기 마감할인 세트"));
  }

  @Test
  void 재고보다_많은_수량을_담으면_409를_반환한다() throws Exception {
    Dish dish =
        dishRepository.save(
            Dish.create(
                1L,
                "치킨마요 마감할인 세트",
                LocalDateTime.now(),
                "마감 임박 할인 상품",
                Category.KOREAN,
                null,
                2L, // 재고 2개
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(5000)));
    Long memberId = 300L;

    String getOrCreateResponse =
        mockMvc
            .perform(get("/api/v1/carts/members/{memberId}", memberId))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long cartId = objectMapper.readTree(getOrCreateResponse).path("data").path("cartId").asLong();

    mockMvc
        .perform(
            post("/api/v1/carts/{cartId}/items", cartId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(dish.getId(), 3L))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("C004"));
  }

  @Test
  void 수량_변경도_재고를_초과하면_409를_반환한다() throws Exception {
    Dish dish =
        dishRepository.save(
            Dish.create(
                1L,
                "치킨마요 마감할인 세트",
                LocalDateTime.now(),
                "마감 임박 할인 상품",
                Category.KOREAN,
                null,
                2L, // 재고 2개
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(5000)));
    Long memberId = 400L;

    String getOrCreateResponse =
        mockMvc
            .perform(get("/api/v1/carts/members/{memberId}", memberId))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long cartId = objectMapper.readTree(getOrCreateResponse).path("data").path("cartId").asLong();

    String addItemResponse =
        mockMvc
            .perform(
                post("/api/v1/carts/{cartId}/items", cartId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(new CartItemAddRequest(dish.getId(), 1L))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long itemId = objectMapper.readTree(addItemResponse).path("data").path("cartItemId").asLong();

    mockMvc
        .perform(
            patch("/api/v1/carts/{cartId}/items/{itemId}", cartId, itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(5L))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("C004"));

    // 실패했으니 수량은 그대로 1이어야 한다
    mockMvc
        .perform(get("/api/v1/carts/members/{memberId}", memberId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].quantity").value(1));
  }
}
