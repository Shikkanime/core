import 'package:shikkanime_admin/core/network/schemas/locale.dart';
import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// One localized text of an aggregate.
const localizationSchema = ProtoSchema({
  1: ProtoField('locale', ProtoKind.enumeration, enumValues: localeValues),
  2: ProtoField('title', ProtoKind.string, nullable: true),
  3: ProtoField('description', ProtoKind.string, nullable: true),
});
