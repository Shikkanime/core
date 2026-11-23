// ignore_for_file: constant_identifier_names
// The constant names are the Kotlin wire values: they must stay UPPER_SNAKE to match the API.
/// Nature of an episode entry, mirroring `fr.shikkanime.models.EpisodeType`.
///
/// The constant names are the Kotlin enum names, which are also the wire values of the JSON,
/// CBOR and Protobuf formats. The declaration order matches the Kotlin enum: the generated
/// Protobuf schema derives its ordinals from it, so do not reorder the constants.
///
/// [unknown] is a client-side fallback for a value the server does not know yet; it is never
/// sent on the wire and excluded from the Protobuf enum values.
enum EpisodeType { EPISODE, FILM, SPECIAL, SUMMARY, SPIN_OFF, unknown }
