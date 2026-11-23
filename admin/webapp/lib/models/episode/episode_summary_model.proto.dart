// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of EpisodeSummaryModel, mirroring the Kotlin DTO field order.
const episodeSummaryModelProtoSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('season', ProtoKind.integer),
  3: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: ['EPISODE', 'FILM', 'SPECIAL', 'SUMMARY', 'SPIN_OFF'],
  ),
  4: ProtoField('number', ProtoKind.integer),
  5: ProtoField('releaseDateTime', ProtoKind.string),
  6: ProtoField('duration', ProtoKind.integer),
  7: ProtoField('title', ProtoKind.string, nullable: true),
  8: ProtoField('releaseCount', ProtoKind.integer),
  9: ProtoField('localizationCount', ProtoKind.integer),
});
