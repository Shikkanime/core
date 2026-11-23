// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of AnimePlatformModel, mirroring the Kotlin DTO field order.
const animePlatformModelProtoSchema = ProtoSchema({
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
});
