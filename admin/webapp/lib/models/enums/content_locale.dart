// ignore_for_file: constant_identifier_names
// The constant names are the Kotlin wire values: they must stay UPPER_SNAKE to match the API.
/// Language and region of a translation or audio track, mirroring
/// `fr.shikkanime.models.Locale`.
///
/// The constant names are the Kotlin enum names, which are also the wire values of the JSON,
/// CBOR and Protobuf formats. The declaration order matches the Kotlin enum: the generated
/// Protobuf schema derives its ordinals from it, so do not reorder the constants.
///
/// [unknown] is a client-side fallback for a value the server does not know yet; it is never
/// sent on the wire and excluded from the Protobuf enum values. Named `ContentLocale` to avoid
/// the clash with `dart:ui`'s `Locale`.
enum ContentLocale {
  JA_JP,
  EN_US,
  FR_FR,
  FR_BE,
  FR_LU,
  FR_CH,
  CA_AD,
  FR_MC,
  DE_DE,
  PL_PL,
  ZH_CN,
  KO_KR,
  unknown,
}
