/// Converts a decoded CBOR value into the JSON-like shape the models parse.
///
/// CBOR maps can expose non-string keys; the administration models expect `Map<String, dynamic>`
/// and `List<dynamic>` exactly like the JSON transport returns.
dynamic normalizeCbor(dynamic value) => switch (value) {
  Map() => value.map<String, dynamic>(
    (key, nested) => MapEntry(key.toString(), normalizeCbor(nested)),
  ),
  List() => value.map(normalizeCbor).toList(),
  _ => value,
};
