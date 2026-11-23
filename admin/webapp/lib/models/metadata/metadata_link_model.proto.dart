// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of MetadataLinkModel, mirroring the Kotlin DTO field order.
const metadataLinkModelProtoSchema = ProtoSchema({
  1: ProtoField(
    'platform',
    ProtoKind.enumeration,
    enumValues: ['ANILIST', 'MYANIMELIST', 'ANIDB'],
  ),
  2: ProtoField('platformId', ProtoKind.string),
  3: ProtoField('platformAnimeId', ProtoKind.string, nullable: true),
  4: ProtoField('isEpisodeLink', ProtoKind.boolean),
  5: ProtoField('url', ProtoKind.string, nullable: true),
});
