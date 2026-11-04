/// Labels the translation files cannot express.
///
/// A server value that maps to a label - a platform, an episode type, an artwork role - is a
/// `select` in `app_en.arb`, so a new language translates it without touching Dart. Only a value
/// derived from a string itself live here.
library;

/// Language code of an audio locale, reduced to its language (`FR_FR` reads `FR`).
String localeLabel(String value) => value.split('_').first;
