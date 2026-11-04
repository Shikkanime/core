import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

import 'package:shikkanime_admin/core/network/schemas/image.dart';
import 'package:shikkanime_admin/core/network/schemas/localization.dart';
import 'package:shikkanime_admin/core/network/schemas/platform.dart';

/// Source link of an anime on a streaming platform.
const animePlatformSchema = ProtoSchema({
  1: ProtoField('platform', ProtoKind.enumeration, enumValues: platformValues),
  2: ProtoField('platformId', ProtoKind.string),
});

/// Compact row of the anime catalog.
const animeSummarySchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('title', ProtoKind.string),
  3: ProtoField('normalizedTitle', ProtoKind.string),
  4: ProtoField(
    'platforms',
    ProtoKind.enumeration,
    repeated: true,
    enumValues: platformValues,
  ),
  5: ProtoField('localizationCount', ProtoKind.integer),
});

/// Full administration view of one anime.
const animeDetailSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('normalizedTitle', ProtoKind.string),
  3: ProtoField('createdAt', ProtoKind.string, nullable: true),
  4: ProtoField('updatedAt', ProtoKind.string, nullable: true),
  5: ProtoField(
    'localizations',
    ProtoKind.message,
    repeated: true,
    message: localizationSchema,
  ),
  6: ProtoField(
    'platforms',
    ProtoKind.message,
    repeated: true,
    message: animePlatformSchema,
  ),
  7: ProtoField(
    'images',
    ProtoKind.message,
    repeated: true,
    message: imageSchema,
  ),
});
