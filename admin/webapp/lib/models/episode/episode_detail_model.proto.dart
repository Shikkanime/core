// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';
import 'package:shikkanime_admin/models/episode/episode_localization_model.proto.dart';
import 'package:shikkanime_admin/models/episode/episode_release_model.proto.dart';
import 'package:shikkanime_admin/models/image/image_model.proto.dart';
import 'package:shikkanime_admin/models/metadata/metadata_link_model.proto.dart';

/// Protobuf schema of EpisodeDetailModel, mirroring the Kotlin DTO field order.
const episodeDetailModelProtoSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('animeId', ProtoKind.string, nullable: true),
  3: ProtoField('season', ProtoKind.integer),
  4: ProtoField(
    'episodeType',
    ProtoKind.enumeration,
    enumValues: ['EPISODE', 'FILM', 'SPECIAL', 'SUMMARY', 'SPIN_OFF'],
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
    message: episodeLocalizationModelProtoSchema,
  ),
  11: ProtoField(
    'releases',
    ProtoKind.message,
    repeated: true,
    message: episodeReleaseModelProtoSchema,
  ),
  12: ProtoField(
    'images',
    ProtoKind.message,
    repeated: true,
    message: imageModelProtoSchema,
  ),
  13: ProtoField(
    'metadataLinks',
    ProtoKind.message,
    repeated: true,
    message: metadataLinkModelProtoSchema,
  ),
  14: ProtoField('displayTitle', ProtoKind.string, nullable: true),
});
