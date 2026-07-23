package kr.lastdish.core.cart.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.cart.application.CartService;
import kr.lastdish.core.cart.presentation.dto.CartItemAddRequest;
import kr.lastdish.core.cart.presentation.dto.CartItemResponse;
import kr.lastdish.core.cart.presentation.dto.CartItemUpdateRequest;
import kr.lastdish.core.cart.presentation.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class CartController {

  private final CartService cartService;

  // 장바구니 상품 추가
  @PostMapping("/{cartId}/items")
  public ApiResponse<CartItemResponse> addItem(
      @PathVariable Long cartId, @Valid @RequestBody CartItemAddRequest request) {
    return ApiResponse.ok(cartService.addItem(cartId, request));
  }

  // 장바구니 상품 수량 변경
  @PatchMapping("/{cartId}/items/{itemId}")
  public ApiResponse<CartItemResponse> updateItemQuantity(
      @PathVariable Long cartId,
      @PathVariable Long itemId,
      @Valid @RequestBody CartItemUpdateRequest request) {
    return ApiResponse.ok(cartService.updateItemQuantity(cartId, itemId, request));
  }

  // 사용자별 장바구니 조회
  @GetMapping("/members/{memberId}")
  public ApiResponse<CartResponse> getCartByMemberId(@PathVariable Long memberId) {
    return ApiResponse.ok(cartService.getCartByMemberId(memberId));
  }

  // 장바구니 상품 삭제
  @DeleteMapping("/{cartId}/items/{itemId}")
  public ResponseEntity<Void> removeItem(@PathVariable Long cartId, @PathVariable Long itemId) {
    cartService.removeItem(cartId, itemId);
    return ResponseEntity.noContent().build();
  }

  // 추후에 사용할 장바구니 비우기 (Cart 자체는 삭제하지 않고 담긴 상품만 비움)
  @DeleteMapping("/{cartId}")
  public ResponseEntity<Void> clearCart(@PathVariable Long cartId) {
    cartService.clearCart(cartId);
    return ResponseEntity.noContent().build();
  }
}
