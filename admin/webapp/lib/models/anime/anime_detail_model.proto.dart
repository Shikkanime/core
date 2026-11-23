// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';
import 'package:shikkanime_admin/models/anime/anime_localization_model.proto.dart';
import 'package:shikkanime_admin/models/anime/anime_platform_model.proto.dart';
import 'package:shikkanime_admin/models/image/image_model.proto.dart';
import 'package:shikkanime_admin/models/metadata/metadata_link_model.proto.dart';

/// Protobuf schema of AnimeDetailModel, mirroring the Kotlin DTO field order.
const animeDetailModelProtoSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('normalizedTitle', ProtoKind.string),
  3: ProtoField('createdAt', ProtoKind.string, nullable: true),
  4: ProtoField('updatedAt', ProtoKind.string, nullable: true),
  5: ProtoField(
    'localizations',
    ProtoKind.message,
    repeated: true,
    message: animeLocalizationModelProtoSchema,
  ),
  6: ProtoField(
    'platforms',
    ProtoKind.message,
    repeated: true,
    message: animePlatformModelProtoSchema,
  ),
  7: ProtoField(
    'images',
    ProtoKind.message,
    repeated: true,
    message: imageModelProtoSchema,
  ),
  8: ProtoField(
    'metadataLinks',
    ProtoKind.message,
    repeated: true,
    message: metadataLinkModelProtoSchema,
  ),
});
