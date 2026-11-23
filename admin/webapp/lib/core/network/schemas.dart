/// Protobuf schemas of the administration DTOs, generated from the annotated models.
///
/// `kotlinx.serialization` numbers a message field by its declaration order, starting at 1. Each
/// `@ProtoMessage` model generates its `*.proto.dart` schema at build time; the field order of the
/// Dart model must mirror the Kotlin DTO declaration order. Kotlin enums are mirrored by Dart
/// enums under `lib/models/enums/` whose constants keep the Kotlin names and declaration order;
/// the generator excludes their `unknown` fallback from the wire values.
library;

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

export 'package:shikkanime_admin/models/anime/anime_detail_model.proto.dart';
export 'package:shikkanime_admin/models/anime/anime_localization_model.proto.dart';
export 'package:shikkanime_admin/models/anime/anime_platform_model.proto.dart';
export 'package:shikkanime_admin/models/anime/anime_summary_model.proto.dart';
export 'package:shikkanime_admin/models/episode/episode_detail_model.proto.dart';
export 'package:shikkanime_admin/models/episode/episode_localization_model.proto.dart';
export 'package:shikkanime_admin/models/episode/episode_release_model.proto.dart';
export 'package:shikkanime_admin/models/episode/episode_summary_model.proto.dart';
export 'package:shikkanime_admin/models/episode/latest_episode_model.proto.dart';
export 'package:shikkanime_admin/models/image/image_model.proto.dart';
export 'package:shikkanime_admin/models/metadata/metadata_link_model.proto.dart';

/// Page envelope shared by every paged endpoint.
///
/// The four pagination fields are written once, exactly like the Kotlin `PageDto`; an endpoint
/// only declares the schema of its rows.
///
/// @param element schema of one row of the page.
/// @return the schema of a page carrying these rows.
ProtoSchema pageSchema(ProtoSchema element) => ProtoSchema({
  1: ProtoField('data', ProtoKind.message, repeated: true, message: element),
  2: ProtoField('page', ProtoKind.integer),
  3: ProtoField('size', ProtoKind.integer),
  4: ProtoField('totalPages', ProtoKind.integer),
  5: ProtoField('totalElements', ProtoKind.integer),
});
