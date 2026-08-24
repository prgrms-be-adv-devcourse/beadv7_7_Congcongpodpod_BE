package kr.lastdish.core.dish.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishRepositoryImplTest {

  @Mock private DishJpaRepository dishJpaRepository;

  private DishRepositoryImpl dishRepository;

  @BeforeEach
  void setUp() {
    dishRepository = new DishRepositoryImpl(dishJpaRepository);
  }

  @Test
  void 삭제되지_않은_Dish가_없으면_D002를_던진다() {
    given(dishJpaRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> dishRepository.findByIdAndIsDeletedFalse(1L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DISH_NOT_FOUND));
  }

  @Test
  void Dish가_없으면_D002를_던진다() {
    given(dishJpaRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> dishRepository.findById(1L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DISH_NOT_FOUND));
  }

  @Test
  void 잠금_조회할_Dish가_없으면_D002를_던진다() {
    given(dishJpaRepository.findWithLockByIdAndIsDeletedFalse(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> dishRepository.findWithLockByIdAndIsDeletedFalse(1L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DISH_NOT_FOUND));
  }
}
