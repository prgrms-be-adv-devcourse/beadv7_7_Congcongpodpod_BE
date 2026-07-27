import 'package:lastdish_app/domain/repository/recent_store_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SharedPrefsStoreStorage implements RecentStoreStorage {
  SharedPrefsStoreStorage(this._prefs);
  final SharedPreferences _prefs;

  static const _storeId = 'store_Id';

  @override
  Future<void> saveLastStoreId(int storeId) async {
    await _prefs.setInt(_storeId, storeId);
  }

  @override
  Future<int?> getLastStoreId() async {
      return _prefs.getInt(_storeId);
    }


  @override
  Future<void> clear() async {
    await _prefs.remove(_storeId);
  }
}