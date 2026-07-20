package kr.lastdish.core.store.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "store_payout_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorePayoutAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payout_account_id")
  private Long id;

  @Column(name = "store_id", nullable = false)
  private Long storeId;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Column(name = "account_holder", nullable = false)
  private String accountHolder;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  public StorePayoutAccount(Long storeId, String accountNumber, String accountHolder) {
    this.storeId = storeId;
    this.accountNumber = accountNumber;
    this.accountHolder = accountHolder;
    this.active = true;
    this.updatedAt = LocalDateTime.now();
    this.deleted = false;
  }

  public void update(String accountNumber, String accountHolder) {
    this.accountNumber = accountNumber;
    this.accountHolder = accountHolder;
    this.updatedAt = LocalDateTime.now();
  }
}
