// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// _ProtoSchemaGenerator
// **************************************************************************

import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Protobuf schema of ImageModel, mirroring the Kotlin DTO field order.
const imageModelProtoSchema = ProtoSchema({
  1: ProtoField(
    'type',
    ProtoKind.enumeration,
    enumValues: ['THUMBNAIL', 'BANNER', 'TITLE'],
  ),
  2: ProtoField('url', ProtoKind.string),
  3: ProtoField('manual', ProtoKind.boolean),
});
