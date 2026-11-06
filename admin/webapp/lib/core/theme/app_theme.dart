import 'package:flutter/material.dart';

/// Material 3 theme of the administration, sharing the palette of the application v2.
///
/// The scaffold is the page background and the canvas the surface of the panels drawn on it, so a
/// card always reads lighter than the page: white on grey in light mode, `#161616` on deep black in
/// dark mode. Secondary text and icons use the grey of the palette in both modes.
abstract final class AppTheme {
  static ThemeData get light => _build(
    brightness: Brightness.light,
    scaffoldBackgroundColor: const Color(0xFFF0F0F0),
    canvasColor: Colors.white,
    primaryColor: Colors.black,
    textColor: Colors.black,
    greyColor: Colors.grey[800]!,
    elevatedButtonBackgroundColor: Colors.white,
    elevatedButtonShadowColor: Colors.grey[300]!,
  );

  static ThemeData get dark => _build(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: Colors.black,
    canvasColor: const Color(0xFF161616),
    primaryColor: Colors.white,
    textColor: Colors.white,
    greyColor: Colors.grey[400]!,
    elevatedButtonBackgroundColor: const Color(0xFF282828),
    elevatedButtonShadowColor: Colors.grey[900]!,
  );

  static ThemeData _build({
    required Brightness brightness,
    required Color scaffoldBackgroundColor,
    required Color canvasColor,
    required Color primaryColor,
    required Color textColor,
    required Color greyColor,
    required Color elevatedButtonBackgroundColor,
    required Color elevatedButtonShadowColor,
  }) {
    const appBarBackgroundOpacity = 0.6;
    const bodyLargeFontSize = 16.0;
    const bodySmallFontSize = 11.0;
    const cardBorderRadius = 24.0;
    const fieldBorderRadius = 16.0;
    const popupMenuBorderRadius = 16.0;

    // The palette is achromatic: the default variant tints the containers with the hue it derives
    // from the seed, which paints the secondary containers pink in light and teal in dark.
    final colorScheme =
        ColorScheme.fromSeed(
          brightness: brightness,
          seedColor: primaryColor,
          primary: primaryColor,
          dynamicSchemeVariant: DynamicSchemeVariant.monochrome,
        ).copyWith(
          surface: canvasColor,
          onSurface: textColor,
          onSurfaceVariant: greyColor,
        );

    return ThemeData(
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: scaffoldBackgroundColor,
      appBarTheme: AppBarTheme(
        backgroundColor: scaffoldBackgroundColor.withValues(
          alpha: appBarBackgroundOpacity,
        ),
        surfaceTintColor: Colors.transparent,
      ),
      canvasColor: canvasColor,
      cardTheme: CardThemeData(
        color: canvasColor,
        surfaceTintColor: Colors.transparent,
        shadowColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(cardBorderRadius)),
        ),
      ),
      textTheme: TextTheme(
        bodyLarge: TextStyle(
          color: textColor,
          fontSize: bodyLargeFontSize,
          fontWeight: FontWeight.bold,
        ),
        bodyMedium: TextStyle(color: greyColor),
        bodySmall: TextStyle(color: greyColor, fontSize: bodySmallFontSize),
      ),
      iconTheme: IconThemeData(color: greyColor),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: primaryColor,
        linearTrackColor: scaffoldBackgroundColor,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: elevatedButtonBackgroundColor,
          foregroundColor: primaryColor,
          shadowColor: elevatedButtonShadowColor,
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(
          backgroundColor: elevatedButtonBackgroundColor,
          foregroundColor: primaryColor,
          shadowColor: elevatedButtonShadowColor,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: canvasColor,
        hintStyle: TextStyle(color: greyColor),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(fieldBorderRadius),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(fieldBorderRadius),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(fieldBorderRadius),
          borderSide: BorderSide(color: primaryColor),
        ),
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: canvasColor,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(
            Radius.circular(popupMenuBorderRadius),
          ),
        ),
      ),
      dialogTheme: DialogThemeData(backgroundColor: canvasColor),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: canvasColor,
        surfaceTintColor: Colors.transparent,
      ),
      dividerTheme: DividerThemeData(color: colorScheme.outlineVariant),
    );
  }
}
