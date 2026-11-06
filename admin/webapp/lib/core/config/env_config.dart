abstract final class EnvConfig {
  /// Base URL of the administration API.
  ///
  /// Empty by default: the interface is served by the admin server itself, so the page origin is
  /// the API origin. `--dart-define=API_BASE_URL=http://localhost:8081` targets a distant server,
  /// which is what `flutter run` needs when the dev server listens on another port.
  static String get apiBaseUrl {
    const configured = String.fromEnvironment('API_BASE_URL');

    return configured.isNotEmpty ? configured : Uri.base.origin;
  }
}
