/// Wire format used by the administration API client.
///
/// JSON is the default because it is readable and always available; Protobuf and CBOR are binary
/// formats that trade readability for bandwidth. The server negotiates the response from the
/// `Accept` header, so every request carries the header of the selected format.
enum ResponseFormat {
  json('application/json'),
  protobuf('application/protobuf'),
  cbor('application/cbor');

  const ResponseFormat(this.acceptHeader);

  /// `Accept` header value identifying this format.
  final String acceptHeader;

  /// Resolves a stored format name, falling back to [json] for an unknown or missing value.
  static ResponseFormat fromName(String? name) =>
      values.firstWhere((format) => format.name == name, orElse: () => json);
}
