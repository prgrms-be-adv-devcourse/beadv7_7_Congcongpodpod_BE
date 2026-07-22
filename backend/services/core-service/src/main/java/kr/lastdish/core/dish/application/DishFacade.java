package kr.lastdish.core.dish.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DishFacade {
  private final DishService dishService;

  public void decreaseStock(Long dishId, Long quantity) {
    dishService.decreaseStock(dishId, quantity);
  }

  public void increaseStock(Long dishId, Long quantity) {
    dishService.increaseStock(dishId, quantity);
  }
}
