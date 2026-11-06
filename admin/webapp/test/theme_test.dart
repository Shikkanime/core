import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/core/theme/app_theme.dart';

/// Contrast of the panel drawn on the page, shared by every screen of the administration.
void main() {
  test('draws a white card on a grey page in light mode', () {
    final theme = AppTheme.light;

    expect(theme.scaffoldBackgroundColor, const Color(0xFFF0F0F0));
    expect(theme.cardTheme.color, Colors.white);
    expect(
      theme.cardTheme.color!.computeLuminance(),
      greaterThan(theme.scaffoldBackgroundColor.computeLuminance()),
    );
  });

  test('draws a #161616 card on a black page in dark mode', () {
    final theme = AppTheme.dark;

    expect(theme.scaffoldBackgroundColor, Colors.black);
    expect(theme.cardTheme.color, const Color(0xFF161616));
    expect(
      theme.cardTheme.color!.computeLuminance(),
      greaterThan(theme.scaffoldBackgroundColor.computeLuminance()),
    );
  });

  test('keeps the card free of the M3 surface tint', () {
    for (final theme in [AppTheme.light, AppTheme.dark]) {
      expect(theme.cardTheme.surfaceTintColor, Colors.transparent);
      expect(theme.cardTheme.elevation, 0);
    }
  });
}
