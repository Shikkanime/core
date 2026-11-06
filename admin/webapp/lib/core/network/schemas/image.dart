import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Artwork roles in the order `kotlinx.serialization` numbers them for Protobuf.
const imageTypeValues = ['THUMBNAIL', 'BANNER', 'TITLE'];

/// One artwork row of an aggregate.
const imageSchema = ProtoSchema({
  1: ProtoField('type', ProtoKind.enumeration, enumValues: imageTypeValues),
  2: ProtoField('url', ProtoKind.string),
});
