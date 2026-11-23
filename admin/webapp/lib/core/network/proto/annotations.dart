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

/// Assigns the Protobuf field number of a model field explicitly.
///
/// By default the generator numbers the fields by declaration order, starting at 1, mirroring
/// `kotlinx.serialization`. When a model must declare its fields in a different order than the
/// Kotlin DTO, annotate **every** field of the class with `@ProtoNumber(n)` using the Kotlin
/// field numbers; the generated schema follows the annotations, not the declaration order.
/// Models without any annotation keep the declaration-order numbering.
class ProtoNumber {
  const ProtoNumber(this.number);

  /// Protobuf field number, matching the Kotlin DTO declaration order.
  final int number;
}

/// Ranks a constant of a Dart enum mirroring a Kotlin enum on the Protobuf wire.
///
/// By default the generator reads the enum constants in declaration order, which must mirror
/// the Kotlin enum declaration order. When the Dart declaration order cannot or should not
/// match, annotate **every** wire constant with `@ProtoOrder(n)`; the generated values follow
/// the annotations, not the declaration order. Only the relative order matters: any distinct
/// increasing numbers produce the same wire contract. The client-side `unknown` fallback is
/// never annotated nor listed.
class ProtoOrder {
  const ProtoOrder(this.order);

  /// Rank of the constant on the wire, lower values first.
  final int order;
}
