// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of AnimeLocalizationModel, mirroring the Kotlin DTO field order.
const animeLocalizationModelProtoSchema = ProtoSchema({
  1: ProtoField(
    'locale',
    ProtoKind.enumeration,
    enumValues: [
      'JA_JP',
      'EN_US',
      'FR_FR',
      'FR_BE',
      'FR_LU',
      'FR_CH',
      'CA_AD',
      'FR_MC',
      'DE_DE',
      'PL_PL',
      'ZH_CN',
      'KO_KR',
    ],
  ),
  2: ProtoField('title', ProtoKind.string, nullable: true),
  3: ProtoField('description', ProtoKind.string, nullable: true),
});
