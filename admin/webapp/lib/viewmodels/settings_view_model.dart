import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shikkanime_admin/core/settings/response_format.dart';

/// Application-wide settings, persisted in the browser's local storage.
class SettingsViewModel extends ChangeNotifier {
  static const _responseFormatKey = 'response_format';

  ResponseFormat _format = ResponseFormat.json;
  bool _loaded = false;

  ResponseFormat get format => _format;

  bool get isLoaded => _loaded;

  /// Loads the stored format; JSON is used when nothing valid is stored yet.
  Future<void> load() async {
    try {
      final preferences = await SharedPreferences.getInstance();
      _format = ResponseFormat.fromName(
        preferences.getString(_responseFormatKey),
      );
    } on Exception catch (error) {
      debugPrint('Unable to load the response format: $error');
      _format = ResponseFormat.json;
    }

    _loaded = true;
    notifyListeners();
  }

  /// Stores the selected format and notifies the API client immediately.
  Future<void> select(ResponseFormat format) async {
    if (_format == format) return;

    _format = format;
    notifyListeners();

    try {
      final preferences = await SharedPreferences.getInstance();
      await preferences.setString(_responseFormatKey, format.name);
    } on Exception catch (error) {
      debugPrint('Unable to persist the response format: $error');
    }
  }
}
