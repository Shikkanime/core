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
    final buffer = StringBuffer()
      ..writeln(
        '/// Protobuf schema of ${model.displayName}, mirroring the Kotlin DTO field order.',
      )
      ..writeln('const ${_schemaName(model.displayName)} = ProtoSchema({');

    for (var index = 0; index < fields.length; index++) {
      buffer.writeln(
        '  ${index + 1}: ${_fieldLiteral(fields[index], selfUri, imports)},',
      );
    }

    buffer.writeln('});');
    return buffer.toString().trimRight();
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
  /// The constants are read in declaration order, which mirrors the Kotlin enum declaration
  /// order the Protobuf ordinals rely on. The client-side `unknown` fallback is excluded: it
  /// is never a wire value.
  List<String>? _enumValuesOf(DartType? type) {
    if (type is! InterfaceType) return null;
    final element = type.element;
    if (element is! EnumElement) return null;

    return element.fields
        .where((field) => field.isEnumConstant && field.name != 'unknown')
        .map((field) => field.displayName)
        .toList();
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
