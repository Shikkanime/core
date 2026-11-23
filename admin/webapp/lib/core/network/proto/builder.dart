// ignore_for_file: depend_on_referenced_packages
// Build-time tooling: this file is only compiled by build_runner, which resolves dev_dependencies.
import 'package:analyzer/dart/element/element.dart';
import 'package:analyzer/dart/element/nullability_suffix.dart';
import 'package:analyzer/dart/element/type.dart';
import 'package:build/build.dart';
import 'package:source_gen/source_gen.dart';

import 'annotations.dart';

/// Generates one `<model>.proto.dart` library per annotated model, carrying the `ProtoSchema`
/// that decodes the Protobuf representation of the Kotlin DTO the model mirrors.
///
/// Field numbers follow the `kotlinx.serialization` convention: declaration order, starting at
/// 1. The Dart model must declare its fields in the same order as the Kotlin DTO.
Builder protoSchemaBuilder(BuilderOptions options) =>
    LibraryBuilder(_ProtoSchemaGenerator(), generatedExtension: '.proto.dart');

class _ProtoSchemaGenerator extends Generator {
  static final _protoMessage = TypeChecker.typeNamed(ProtoMessage);
  static final _protoNumber = TypeChecker.typeNamed(ProtoNumber);
  static final _protoOrder = TypeChecker.typeNamed(ProtoOrder);

  @override
  String? generate(LibraryReader library, BuildStep buildStep) {
    final models = library.classes
        .where((element) => _protoMessage.hasAnnotationOf(element))
        .toList();
    if (models.isEmpty) return null;

    final package = buildStep.inputId.package;
    final decoderImport = 'package:$package/core/network/protobuf_decoder.dart';
    final selfUri = _selfProtoUri(buildStep.inputId, package);
    final messageImports = <String>{};
    final declarations = StringBuffer();

    for (final model in models) {
      declarations.writeln(_schemaFor(model, selfUri, messageImports));
      declarations.writeln();
    }

    final imports = [decoderImport, ...messageImports.toList()..sort()];
    return '${imports.map((uri) => "import '$uri';").join('\n')}\n\n'
        '${declarations.toString().trimRight()}\n';
  }

  /// Emits the `ProtoSchema` constant of [model] and registers the imports it needs.
  String _schemaFor(ClassElement model, String selfUri, Set<String> imports) {
    final fields = model.fields
        .where((field) => !field.isStatic && !field.displayName.startsWith('_'))
        .toList();
    final ordered = _fieldNumbers(model, fields);
    final buffer = StringBuffer()
      ..writeln(
        '/// Protobuf schema of ${model.displayName}, mirroring the Kotlin DTO field order.',
      )
      ..writeln('const ${_schemaName(model.displayName)} = ProtoSchema({');

    for (final entry in ordered) {
      buffer.writeln(
        '  ${entry.value}: ${_fieldLiteral(entry.key, selfUri, imports)},',
      );
    }

    buffer.writeln('});');
    return buffer.toString().trimRight();
  }

  /// Maps each field of [model] to its Protobuf field number, sorted by number.
  ///
  /// `@ProtoNumber` annotations win when every field carries one; otherwise fields are numbered
  /// by declaration order, starting at 1, mirroring `kotlinx.serialization`.
  List<MapEntry<FieldElement, int>> _fieldNumbers(
    ClassElement model,
    List<FieldElement> fields,
  ) {
    final annotated = <FieldElement, int>{};
    for (final field in fields) {
      final object = _protoNumber.firstAnnotationOf(field);
      if (object == null) continue;

      final number = object.getField('number')?.toIntValue();
      if (number == null) {
        throw StateError(
          '@ProtoNumber on "${model.displayName}.${field.name}" requires an int',
        );
      }
      annotated[field] = number;
    }

    if (annotated.isNotEmpty && annotated.length != fields.length) {
      throw StateError(
        '@ProtoNumber must annotate every field of ${model.displayName} or none',
      );
    }

    final numbers = annotated.isEmpty
        ? {
            for (var index = 0; index < fields.length; index++)
              fields[index]: index + 1,
          }
        : annotated;

    final seen = <int>{};
    for (final number in numbers.values) {
      if (!seen.add(number)) {
        throw StateError(
          'Duplicate Protobuf field number $number in ${model.displayName}',
        );
      }
    }

    final entries = numbers.entries.toList()
      ..sort((a, b) => a.value.compareTo(b.value));
    return entries;
  }

  /// Emits the `ProtoField` literal of one model field.
  String _fieldLiteral(
    FieldElement field,
    String selfUri,
    Set<String> imports,
  ) {
    final type = field.type;
    final nullable = type.nullabilitySuffix == NullabilitySuffix.question;

    if (_isCoreType(type, 'List')) {
      final argument = (type as InterfaceType).typeArguments.singleOrNull;
      final enumValues = _enumValuesOf(argument);
      if (enumValues != null) {
        return _literal(
          field.displayName,
          'enumeration',
          repeated: true,
          enumValues: enumValues,
        );
      }
      final spec = _elementSpec(argument, field, selfUri, imports);
      return _literal(
        field.displayName,
        spec.kind,
        repeated: true,
        message: spec.message,
      );
    }

    final enumValues = _enumValuesOf(type);
    if (enumValues != null) {
      return _literal(
        field.displayName,
        'enumeration',
        nullable: nullable,
        enumValues: enumValues,
      );
    }

    final spec = _elementSpec(type, field, selfUri, imports);
    return _literal(
      field.displayName,
      spec.kind,
      nullable: nullable,
      message: spec.message,
    );
  }

  /// Resolves the protobuf kind of a non-list element type.
  ///
  /// Nested administration models become `ProtoKind.message` and register the import of their
  /// generated schema.
  ({String kind, String? message}) _elementSpec(
    DartType? type,
    FieldElement field,
    String selfUri,
    Set<String> imports,
  ) {
    if (type != null) {
      if (_isCoreType(type, 'String') || _isCoreType(type, 'DateTime')) {
        return (kind: 'string', message: null);
      }
      if (_isCoreType(type, 'int')) return (kind: 'integer', message: null);
      if (_isCoreType(type, 'bool')) return (kind: 'boolean', message: null);
      if (type is InterfaceType) {
        final element = type.element;
        if (element is ClassElement && _protoMessage.hasAnnotationOf(element)) {
          final uri = _generatedUri(element);
          if (uri != selfUri) imports.add(uri);
          return (kind: 'message', message: _schemaName(element.displayName));
        }
      }
    }
    throw StateError(
      'Cannot map field "${field.displayName}" of type '
      '${type?.getDisplayString() ?? 'unknown'} '
      'in ${field.enclosingElement.displayName} to a protobuf kind',
    );
  }

  /// Reads the wire values of a Dart enum type, or null when [type] is not an enum.
  ///
  /// `@ProtoOrder` annotations on the constants win when every wire constant carries one; the
  /// values are then emitted sorted by rank. Otherwise the constants are read in declaration
  /// order, which mirrors the Kotlin enum declaration order the Protobuf ordinals rely on. The
  /// client-side `unknown` fallback is excluded: it is never a wire value.
  List<String>? _enumValuesOf(DartType? type) {
    if (type is! InterfaceType) return null;
    final element = type.element;
    if (element is! EnumElement) return null;

    final constants = element.fields
        .where((field) => field.isEnumConstant && field.name != 'unknown')
        .toList();
    if (constants.isEmpty) return null;

    final ranks = <FieldElement, int>{};
    for (final constant in constants) {
      final object = _protoOrder.firstAnnotationOf(constant);
      if (object == null) continue;

      final order = object.getField('order')?.toIntValue();
      if (order == null) {
        throw StateError(
          '@ProtoOrder on ${element.displayName}.${constant.name} requires an int',
        );
      }
      ranks[constant] = order;
    }

    if (ranks.isNotEmpty && ranks.length != constants.length) {
      throw StateError(
        '@ProtoOrder must annotate every wire constant of ${element.displayName} or none',
      );
    }

    if (ranks.isEmpty) {
      return constants.map((field) => field.displayName).toList();
    }

    final seen = <int>{};
    for (final order in ranks.values) {
      if (!seen.add(order)) {
        throw StateError(
          'Duplicate @ProtoOrder rank $order in ${element.displayName}',
        );
      }
    }

    final sorted = ranks.entries.toList()
      ..sort((a, b) => a.value.compareTo(b.value));
    return sorted.map((entry) => entry.key.displayName).toList();
  }

  /// Formats one `ProtoField(...)` expression.
  String _literal(
    String name,
    String kind, {
    bool repeated = false,
    bool nullable = false,
    List<String>? enumValues,
    String? message,
  }) {
    final arguments = StringBuffer("'$name', ProtoKind.$kind");
    if (repeated) arguments.write(', repeated: true');
    if (nullable) arguments.write(', nullable: true');
    if (enumValues != null) {
      arguments.write(', enumValues: ${_stringList(enumValues)}');
    }
    if (message != null) arguments.write(', message: $message');
    return 'ProtoField($arguments)';
  }

  /// Formats a Dart string list literal inside the const schema map.
  String _stringList(List<String> values) =>
      '[${values.map((v) => "'${v.replaceAll("'", "\\'")}'").join(', ')}]';

  /// Checks that [type] is `dart:core` type [name] (`String`, `int`, `bool`, ...).
  bool _isCoreType(DartType? type, String name) =>
      type is InterfaceType &&
      type.element.name == name &&
      type.element.library.uri.isScheme('dart') == true;

  /// Package URI of the `.proto.dart` file generated for [type]'s library.
  String _generatedUri(ClassElement type) {
    final uri = type.library.uri;
    if (!uri.isScheme('package') || !uri.path.endsWith('.dart')) {
      throw StateError('Cannot locate the generated schema of ${type.name}');
    }
    final path =
        '${uri.path.substring(0, uri.path.length - '.dart'.length)}.proto.dart';
    return Uri(scheme: 'package', path: path).toString();
  }

  /// Package URI of the `.proto.dart` file generated for [inputId]'s library.
  String _selfProtoUri(AssetId inputId, String package) {
    var path = inputId.path;
    if (path.startsWith('lib/')) path = path.substring('lib/'.length);
    if (path.endsWith('.dart')) {
      path = '${path.substring(0, path.length - '.dart'.length)}.proto.dart';
    }
    return 'package:$package/$path';
  }

  /// Lower camel case of the model class name, used as the schema constant prefix.
  String _schemaName(String className) =>
      '${className[0].toLowerCase()}${className.substring(1)}ProtoSchema';
}
