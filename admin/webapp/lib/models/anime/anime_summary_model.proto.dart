// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of AnimeSummaryModel, mirroring the Kotlin DTO field order.
const animeSummaryModelProtoSchema = ProtoSchema({
  1: ProtoField('id', ProtoKind.string),
  2: ProtoField('title', ProtoKind.string),
  3: ProtoField('normalizedTitle', ProtoKind.string),
  4: ProtoField(
    'platforms',
    ProtoKind.enumeration,
    repeated: true,
    enumValues: [
      'ANIMATION_DIGITAL_NETWORK',
      'CRUNCHYROLL',
      'DISNEY_PLUS',
      'NETFLIX',
      'PRIME_VIDEO',
    ],
  ),
  5: ProtoField('localizationCount', ProtoKind.integer),
});
