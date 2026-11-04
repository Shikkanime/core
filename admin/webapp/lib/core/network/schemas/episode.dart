import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

import 'package:shikkanime_admin/core/network/schemas/image.dart';
import 'package:shikkanime_admin/core/network/schemas/locale.dart';
import 'package:shikkanime_admin/core/network/schemas/localization.dart';
import 'package:shikkanime_admin/core/network/schemas/platform.dart';

/// Episode categories in the order `kotlinx.serialization` numbers them for Protobuf.
const episodeTypeValues = ['EPISODE', 'FILM', 'SPECIAL', 'SUMMARY', 'SPIN_OFF'];

/// Compact row of the episode list of an anime.
const episodeSummarySchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('season', ProtoKind.integer),
  3: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: episodeTypeValues,
  ),
  4: ProtoField('number', ProtoKind.integer),
  5: ProtoField('releaseDateTime', ProtoKind.string),
  6: ProtoField('duration', ProtoKind.integer),
  7: ProtoField('title', ProtoKind.string, nullable: true),
  8: ProtoField('releaseCount', ProtoKind.integer),
  9: ProtoField('localizationCount', ProtoKind.integer),
});

/// One platform release of an episode.
const releaseSchema = ProtoSchema({
  1: ProtoField('platform', ProtoKind.enumeration, enumValues: platformValues),
  2: ProtoField('platformId', ProtoKind.string),
  3: ProtoField('url', ProtoKind.string),
  4: ProtoField('audioLocale', ProtoKind.enumeration, enumValues: localeValues),
  5: ProtoField('uncensored', ProtoKind.boolean),
  6: ProtoField('releaseDateTime', ProtoKind.string),
});

/// Full administration view of one episode.
const episodeDetailSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('animeId', ProtoKind.string, nullable: true),
  3: ProtoField('season', ProtoKind.integer),
  4: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: episodeTypeValues,
  ),
  5: ProtoField('number', ProtoKind.integer),
  6: ProtoField('releaseDateTime', ProtoKind.string),
  7: ProtoField('duration', ProtoKind.integer),
  8: ProtoField('createdAt', ProtoKind.string, nullable: true),
  9: ProtoField('updatedAt', ProtoKind.string, nullable: true),
  10: ProtoField(
    'localizations',
    ProtoKind.message,
    repeated: true,
    message: localizationSchema,
  ),
  11: ProtoField(
    'releases',
    ProtoKind.message,
    repeated: true,
    message: releaseSchema,
  ),
  12: ProtoField(
    'images',
    ProtoKind.message,
    repeated: true,
    message: imageSchema,
  ),
});

/// Row of the release list, with the anime it belongs to.
const latestEpisodeSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('animeId', ProtoKind.string),
  3: ProtoField('animeTitle', ProtoKind.string),
  4: ProtoField('season', ProtoKind.integer),
  5: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: episodeTypeValues,
  ),
  6: ProtoField('number', ProtoKind.integer),
  7: ProtoField('releaseDateTime', ProtoKind.string),
  8: ProtoField('title', ProtoKind.string, nullable: true),
  9: ProtoField('releaseCount', ProtoKind.integer),
  10: ProtoField('localizationCount', ProtoKind.integer),
});
