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

/// Overrides the Protobuf wire order of a Dart enum mirroring a Kotlin enum.
///
/// By default the generator reads the enum constants in declaration order, which must mirror
/// the Kotlin enum declaration order. Annotate the enum with this when the Dart declaration
/// order cannot or should not match: [values] lists the wire values — the Kotlin constant
/// names — in their Kotlin declaration order. The client-side `unknown` fallback is never
/// listed.
class ProtoEnumOrder {
  const ProtoEnumOrder(this.values);

  /// Wire values in their Kotlin declaration order.
  final List<String> values;
}
