import 'dart:convert';

import 'package:cbor/simple.dart';
import 'package:http/http.dart' as http;
import 'package:shikkanime_admin/core/config/env_config.dart';
import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/core/network/cbor_decoder.dart';
import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';
import 'package:shikkanime_admin/core/settings/response_format.dart';
import 'package:shikkanime_admin/viewmodels/settings_view_model.dart';

/// Single entry point to the administration API.
///
/// Builds the URI from [EnvConfig.apiBaseUrl], enforces [timeout], checks the status and decodes
/// the body with the format selected in [SettingsViewModel]: JSON, Protobuf or CBOR. The decoded
/// value keeps the map shape the models already parse, whatever the transport.
class HttpClient {
  HttpClient(
    this._settings, {
    http.Client? client,
    String? baseUrl,
    this.timeout = const Duration(seconds: 15),
  }) : _client = client ?? http.Client(),
       _baseUrl = baseUrl ?? EnvConfig.apiBaseUrl;

  final SettingsViewModel _settings;
  final http.Client _client;
  final String _baseUrl;
  final Duration timeout;

  Uri _buildUri(String endpoint, [Map<String, Object>? queryParameters]) {
    final baseUri = Uri.parse('$_baseUrl/$endpoint');

    if (queryParameters == null || queryParameters.isEmpty) {
      return baseUri;
    }

    return baseUri.replace(
      queryParameters: queryParameters.map(
        (key, value) => MapEntry(key, value.toString()),
      ),
    );
  }

  ApiResult<T> _parseBody<T>(http.Response response, ProtoSchema? protoSchema) {
    try {
      final decoded = switch (_settings.format) {
        ResponseFormat.json => jsonDecode(utf8.decode(response.bodyBytes)),
        ResponseFormat.cbor => normalizeCbor(cbor.decode(response.bodyBytes)),
        ResponseFormat.protobuf => _decodeProtobuf(response, protoSchema),
      };

      if (decoded is! T) {
        return ApiFailure(
          'Invalid response type: expected $T, got ${decoded.runtimeType}',
        );
      }

      return ApiSuccess(decoded);
    } on Exception catch (error) {
      return ApiFailure('Invalid ${_settings.format.name} response: $error');
    }
  }

  dynamic _decodeProtobuf(http.Response response, ProtoSchema? protoSchema) {
    if (protoSchema == null) {
      throw const FormatException('Protobuf response without a schema');
    }

    return decodeProtobuf(response.bodyBytes, protoSchema);
  }

  Future<ApiResult<T>> get<T>(
    String endpoint, {
    Map<String, Object>? queryParameters,
    ProtoSchema? protoSchema,
  }) async {
    final uri = _buildUri(endpoint, queryParameters);

    try {
      final response = await _client
          .get(uri, headers: {'Accept': _settings.format.acceptHeader})
          .timeout(timeout);

      if (response.statusCode < 200 || response.statusCode >= 300) {
        return ApiFailure(response.reasonPhrase, response.statusCode);
      }

      return _parseBody<T>(response, protoSchema);
    } on Exception catch (error) {
      return ApiFailure(error.toString());
    }
  }
}
