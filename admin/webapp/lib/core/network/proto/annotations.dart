/// Marks an administration model whose fields mirror a Kotlin DTO serialized as Protobuf.
///
/// The `protoSchemaBuilder` reads the declared fields in order — `kotlinx.serialization` numbers
/// message fields by declaration order, starting at 1 — and emits the matching `ProtoSchema` in
/// `<model>.proto.dart`. The Dart model must declare its fields in the same order as the Kotlin
/// DTO, and a String field carrying a Kotlin enum must be marked with [ProtoEnum].
class ProtoMessage {
  const ProtoMessage();
}

/// Marks a String (or `List<String>`) field whose value is the name of a Kotlin enum constant.
///
/// [values] must list the enum constants in their Kotlin declaration order: Protobuf carries the
/// ordinal, so the order is part of the wire contract.
class ProtoEnum {
  const ProtoEnum(this.values);

  /// Enum constants in their Kotlin declaration order.
  final List<String> values;
}
