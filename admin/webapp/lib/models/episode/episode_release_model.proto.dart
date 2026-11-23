// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of EpisodeReleaseModel, mirroring the Kotlin DTO field order.
const episodeReleaseModelProtoSchema = ProtoSchema({
  1: ProtoField(
    'platform',
    ProtoKind.enumeration,
    enumValues: [
      'ANIMATION_DIGITAL_NETWORK',
      'CRUNCHYROLL',
      'DISNEY_PLUS',
      'NETFLIX',
      'PRIME_VIDEO',
    ],
  ),
  2: ProtoField('platformId', ProtoKind.string),
  3: ProtoField('url', ProtoKind.string),
  4: ProtoField(
    'audioLocale',
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
  5: ProtoField('uncensored', ProtoKind.boolean),
  6: ProtoField('releaseDateTime', ProtoKind.string),
});
