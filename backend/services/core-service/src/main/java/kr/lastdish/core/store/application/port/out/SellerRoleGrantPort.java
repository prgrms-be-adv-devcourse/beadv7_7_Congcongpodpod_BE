package kr.lastdish.core.store.application.port.out;

// 역방향 참조를 없애기 위한 PORT
public interface SellerRoleGrantPort {

  void grantSellerRole(Long memberId);
}
