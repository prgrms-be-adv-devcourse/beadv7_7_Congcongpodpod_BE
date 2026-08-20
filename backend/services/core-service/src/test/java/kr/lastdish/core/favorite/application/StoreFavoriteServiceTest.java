package kr.lastdish.core.favorite.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.favorite.application.port.out.FavoriteStoreQueryPort;
import kr.lastdish.core.favorite.domain.StoreFavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreFavoriteServiceTest {

  @Mock private StoreFavoriteRepository storeFavoriteRepository;
  @Mock private FavoriteStoreQueryPort favoriteStoreQueryPort;
  @InjectMocks private StoreFavoriteService storeFavoriteService;

  @Test
  void 존재하는_매장을_찜하면_저장한다() {
    when(favoriteStoreQueryPort.existsById(1L)).thenReturn(true);

    storeFavoriteService.addFavorite(10L, 1L);

    verify(storeFavoriteRepository).createIfAbsent(10L, 1L);
  }

  @Test
  void 없는_매장을_찜하면_404를_던진다() {
    when(favoriteStoreQueryPort.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> storeFavoriteService.addFavorite(10L, 99L))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void 찜_해제는_찜이_있을_때만_삭제한다() {
    storeFavoriteService.removeFavorite(10L, 1L);

    verify(storeFavoriteRepository).deleteByMemberIdAndStoreId(10L, 1L);
  }
}
