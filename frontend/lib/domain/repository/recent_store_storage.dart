abstract interface class RecentStoreStorage {
  Future<void> saveLastStoreId(int storeId);

  Future<int?> getLastStoreId();

  Future<void> clear();
}
