import 'dart:convert';
import 'dart:typed_data';

/// Kind of value a protobuf field carries.
enum ProtoKind { string, integer, boolean, enumeration, message }

/// Description of one field of a protobuf message.
class ProtoField {
  const ProtoField(
    this.name,
    this.kind, {
    this.repeated = false,
    this.nullable = false,
    this.enumValues,
    this.message,
  });

  final String name;
  final ProtoKind kind;
  final bool repeated;
  final bool nullable;
  final List<String>? enumValues;
  final ProtoSchema? message;
}

/// Description of a protobuf message, indexed by field number.
class ProtoSchema {
  const ProtoSchema(this.fields);

  final Map<int, ProtoField> fields;
}

/// Decodes a protobuf payload into the map shape the JSON models already parse.
///
/// The administration API serializes Kotlin DTOs with kotlinx.serialization: field numbers follow
/// the declaration order of each DTO. [schema] describes that contract, so the binary payload can
/// feed the same `fromJson` factories as the JSON and CBOR transports.
Map<String, dynamic> decodeProtobuf(Uint8List bytes, ProtoSchema schema) {
  final values = <String, dynamic>{
    for (final field in schema.fields.values) field.name: _defaultValue(field),
  };
  final reader = _ProtoReader(bytes);

  while (!reader.isAtEnd) {
    final key = reader.readVarint();
    final number = key >> 3;
    final wireType = key & 7;
    final field = schema.fields[number];

    if (field == null) {
      reader.skip(wireType);
      continue;
    }

    // Kotlinx serialization writes repeated scalars unpacked, but accept the packed form too.
    if (wireType == 2 &&
        field.repeated &&
        (field.kind == ProtoKind.integer ||
            field.kind == ProtoKind.enumeration)) {
      final packed = _ProtoReader(Uint8List.fromList(reader.readBytes()));

      while (!packed.isAtEnd) {
        _add(values, field, _readScalar(packed, field));
      }

      continue;
    }

    switch (field.kind) {
      case ProtoKind.string:
        if (wireType != 2) {
          reader.skip(wireType);
        } else {
          _add(values, field, reader.readString());
        }
      case ProtoKind.integer:
      case ProtoKind.boolean:
      case ProtoKind.enumeration:
        if (wireType != 0) {
          reader.skip(wireType);
        } else {
          _add(values, field, _readScalar(reader, field));
        }
      case ProtoKind.message:
        if (wireType != 2) {
          reader.skip(wireType);
        } else {
          final nested = decodeProtobuf(
            Uint8List.fromList(reader.readBytes()),
            field.message!,
          );
          _add(values, field, nested);
        }
    }
  }

  return values;
}

dynamic _defaultValue(ProtoField field) {
  if (field.repeated) return <dynamic>[];

  return switch (field.kind) {
    ProtoKind.string => field.nullable ? null : '',
    ProtoKind.integer => 0,
    ProtoKind.boolean => false,
    ProtoKind.enumeration => field.enumValues!.first,
    ProtoKind.message => null,
  };
}

dynamic _readScalar(_ProtoReader reader, ProtoField field) {
  final value = reader.readVarint();

  return switch (field.kind) {
    ProtoKind.integer => value,
    ProtoKind.boolean => value != 0,
    ProtoKind.enumeration =>
      value >= 0 && value < field.enumValues!.length
          ? field.enumValues![value]
          : '$value',
    _ => value,
  };
}

void _add(Map<String, dynamic> values, ProtoField field, dynamic value) {
  if (field.repeated) {
    (values[field.name] as List<dynamic>).add(value);
  } else {
    values[field.name] = value;
  }
}

class _ProtoReader {
  _ProtoReader(this._bytes);

  final Uint8List _bytes;
  int _offset = 0;

  bool get isAtEnd => _offset >= _bytes.length;

  /// Reads one base-128 varint.
  int readVarint() {
    var result = 0;
    var factor = 1;

    while (true) {
      if (_offset >= _bytes.length) {
        throw const FormatException('Truncated protobuf varint');
      }

      final byte = _bytes[_offset++];
      result += (byte & 0x7f) * factor;

      if (byte & 0x80 == 0) return result;
      factor *= 128;

      if (factor > 0x1000000000000000) {
        throw const FormatException('Protobuf varint is too long');
      }
    }
  }

  /// Reads a length-delimited field as UTF-8 text.
  String readString() => utf8.decode(readBytes());

  /// Reads a length-delimited field as raw bytes.
  List<int> readBytes() {
    final length = readVarint();

    if (length < 0 || _offset + length > _bytes.length) {
      throw const FormatException('Truncated protobuf field');
    }

    final value = _bytes.sublist(_offset, _offset + length);
    _offset += length;

    return value;
  }

  /// Skips the value of the field whose wire type is [wireType].
  void skip(int wireType) {
    switch (wireType) {
      case 0:
        readVarint();
      case 1:
        _offset += 8;
      case 2:
        readBytes();
      case 5:
        _offset += 4;
      default:
        throw FormatException('Unsupported protobuf wire type $wireType');
    }
  }
}
