/// Marks an administration model whose fields mirror a Kotlin DTO serialized as Protobuf.
///
/// The `protoSchemaBuilder` reads the declared fields in order — `kotlinx.serialization` numbers
/// message fields by declaration order, starting at 1 — and emits the matching `ProtoSchema` in
/// `<model>.proto.dart`. The Dart model must declare its fields in the same order as the Kotlin
/// DTO. Enum fields are real Dart enums whose constants mirror the Kotlin enum names and
/// declaration order; their Protobuf ordinals are derived from the Dart declaration order.
class ProtoMessage {
  const ProtoMessage();
}
