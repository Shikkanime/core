import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';

/// Page envelope shared by every paged endpoint.
///
/// The four pagination fields are written once, exactly like the Kotlin `PageDto`; an endpoint
/// only declares the schema of its rows.
///
/// @param element schema of one row of the page.
/// @return the schema of a page carrying these rows.
ProtoSchema pageSchema(ProtoSchema element) => ProtoSchema({
  1: ProtoField('data', ProtoKind.message, repeated: true, message: element),
  2: ProtoField('page', ProtoKind.integer),
  3: ProtoField('size', ProtoKind.integer),
  4: ProtoField('totalPages', ProtoKind.integer),
  5: ProtoField('totalElements', ProtoKind.integer),
});
