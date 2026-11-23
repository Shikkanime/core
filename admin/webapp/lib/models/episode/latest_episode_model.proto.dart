// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of LatestEpisodeModel, mirroring the Kotlin DTO field order.
const latestEpisodeModelProtoSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('animeId', ProtoKind.string),
  3: ProtoField('animeTitle', ProtoKind.string),
  4: ProtoField('season', ProtoKind.integer),
  5: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: ['EPISODE', 'FILM', 'SPECIAL', 'SUMMARY', 'SPIN_OFF'],
  ),
  6: ProtoField('number', ProtoKind.integer),
  7: ProtoField('releaseDateTime', ProtoKind.string),
  8: ProtoField('title', ProtoKind.string, nullable: true),
  9: ProtoField('releaseCount', ProtoKind.integer),
  10: ProtoField('localizationCount', ProtoKind.integer),
});
