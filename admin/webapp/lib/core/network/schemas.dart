/// Protobuf schemas of the administration DTOs, one file per aggregate.
///
/// `kotlinx.serialization` numbers a message field by its declaration order, starting at 1. These
/// schemas mirror the Kotlin DTOs (`AdminAnimeSummaryDto`, `AdminEpisodeDetailDto`, ...); when a
/// field is added, removed or reordered on the server, the matching schema changes in the same
/// commit. The enum lists repeat the Kotlin declaration order, because Protobuf carries the ordinal.
library;

export 'package:shikkanime_admin/core/network/schemas/anime.dart';
export 'package:shikkanime_admin/core/network/schemas/episode.dart';
export 'package:shikkanime_admin/core/network/schemas/image.dart';
export 'package:shikkanime_admin/core/network/schemas/locale.dart';
export 'package:shikkanime_admin/core/network/schemas/localization.dart';
export 'package:shikkanime_admin/core/network/schemas/page.dart';
export 'package:shikkanime_admin/core/network/schemas/platform.dart';
