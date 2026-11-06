import 'dart:convert';
import 'dart:typed_data';

import 'package:cbor/simple.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shikkanime_admin/core/network/schemas.dart';
import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/core/network/cbor_decoder.dart';
import 'package:shikkanime_admin/core/network/http_client.dart';
import 'package:shikkanime_admin/core/network/protobuf_decoder.dart';
import 'package:shikkanime_admin/core/settings/response_format.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';
import 'package:shikkanime_admin/models/episode/episode_detail_model.dart';
import 'package:shikkanime_admin/viewmodels/settings_view_model.dart';
import 'package:shikkanime_admin/views/settings/settings_view.dart';

const _animesPageProtobuf =
    'ClMKJDAwMDAwMDAzLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMRIUTCdBdHRhcXVlIGRlcyBUaXRhbnMaD0F0dGFjayBvbiBUaXRhbiABIAAoAhABGAEgAygD';
const _animeDetailProtobuf =
    'CiQwMDAwMDAwMS0xMTExLTExMTEtMTExMS0xMTExMTExMTExMTESCU9uZSBQaWVjZRoQMjAyNS0wMS0wMVQxMDowMCIQMjAyNS0wNi0wMVQxMjowMCozCAISHkxlIHBpcmF0ZSBhdSBjaGFwZWF1IGRlIHBhaWxsZRoPR29tdSBHb211IG5vIE1pKh4IARIJT25lIFBpZWNlGg9Hb211IEdvbXUgbm8gTWkyCAgBEgQxMDAwMggIABIEMTAwMTowCAESLGh0dHBzOi8vY2RuLmV4YW1wbGUuY29tL29uZS1waWVjZS1iYW5uZXIuanBnOi8IABIraHR0cHM6Ly9jZG4uZXhhbXBsZS5jb20vb25lLXBpZWNlLXRodW1iLmpwZw==';
const _episodesPageProtobuf =
    'ClEKJDAwMDAwMDAxLTU1NTUtNTU1NS01NTU1LTU1NTU1NTU1NTU1NRABGAAgASoQMjAyNC0wMS0wN1QxMjowMDCgCzoKw4lwaXNvZGUgMUACSAIQARgBIBkoGQ==';
const _episodeDetailProtobuf =
    'CiQwMDAwMDAwMS01NTU1LTU1NTUtNTU1NS01NTU1NTU1NTU1NTUSJDAwMDAwMDAxLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMRgBIAAoATIQMjAyNC0wMS0wN1QxMjowMDigC0IQMjAyNS0wMS0wMVQxMDowMEoQMjAyNS0wNi0wMVQxMjowMFIpCAISCsOJcGlzb2RlIDEaGURlc2NyaXB0aW9uIG9mIGVwaXNvZGUgMS5SKAgBEglFcGlzb2RlIDEaGURlc2NyaXB0aW9uIG9mIGVwaXNvZGUgMS5aRggBEgQ1MDEwGiZodHRwczovL3BsYXRmb3JtLmV4YW1wbGUuY29tL3dhdGNoLzEtMCACKAAyEDIwMjQtMDEtMDdUMTI6MDBaRggAEgQ1MDExGiZodHRwczovL3BsYXRmb3JtLmV4YW1wbGUuY29tL3dhdGNoLzEtMSAAKAEyEDIwMjQtMDEtMDdUMTI6MDA=';
const _animesPageCbor =
    'v2RkYXRhn79iaWR4JDAwMDAwMDAzLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMWV0aXRsZXRMJ0F0dGFxdWUgZGVzIFRpdGFuc29ub3JtYWxpemVkVGl0bGVvQXR0YWNrIG9uIFRpdGFuaXBsYXRmb3Jtc59rQ1JVTkNIWVJPTEx4GUFOSU1BVElPTl9ESUdJVEFMX05FVFdPUkv/cWxvY2FsaXphdGlvbkNvdW50Av//ZHBhZ2UBZHNpemUBanRvdGFsUGFnZXMDbXRvdGFsRWxlbWVudHMD/w==';
const _animeDetailCbor =
    'v2JpZHgkMDAwMDAwMDEtMTExMS0xMTExLTExMTEtMTExMTExMTExMTExb25vcm1hbGl6ZWRUaXRsZWlPbmUgUGllY2VpY3JlYXRlZEF0cDIwMjUtMDEtMDFUMTA6MDBpdXBkYXRlZEF0cDIwMjUtMDYtMDFUMTI6MDBtbG9jYWxpemF0aW9uc5+/ZmxvY2FsZWVGUl9GUmV0aXRsZXgeTGUgcGlyYXRlIGF1IGNoYXBlYXUgZGUgcGFpbGxla2Rlc2NyaXB0aW9ub0dvbXUgR29tdSBubyBNaf+/ZmxvY2FsZWVFTl9VU2V0aXRsZWlPbmUgUGllY2VrZGVzY3JpcHRpb25vR29tdSBHb211IG5vIE1p//9pcGxhdGZvcm1zn79ocGxhdGZvcm1rQ1JVTkNIWVJPTExqcGxhdGZvcm1JZGQxMDAw/79ocGxhdGZvcm14GUFOSU1BVElPTl9ESUdJVEFMX05FVFdPUktqcGxhdGZvcm1JZGQxMDAx//9maW1hZ2Vzn79kdHlwZWZCQU5ORVJjdXJseCxodHRwczovL2Nkbi5leGFtcGxlLmNvbS9vbmUtcGllY2UtYmFubmVyLmpwZ/+/ZHR5cGVpVEhVTUJOQUlMY3VybHgraHR0cHM6Ly9jZG4uZXhhbXBsZS5jb20vb25lLXBpZWNlLXRodW1iLmpwZ////w==';
const _episodesPageCbor =
    'v2RkYXRhn79iaWR4JDAwMDAwMDAxLTU1NTUtNTU1NS01NTU1LTU1NTU1NTU1NTU1NWZzZWFzb24Ba2VwaXNvZGVUeXBlZ0VQSVNPREVmbnVtYmVyAW9yZWxlYXNlRGF0ZVRpbWVwMjAyNC0wMS0wN1QxMjowMGhkdXJhdGlvbhkFoGV0aXRsZWrDiXBpc29kZSAxbHJlbGVhc2VDb3VudAJxbG9jYWxpemF0aW9uQ291bnQC//9kcGFnZQFkc2l6ZQFqdG90YWxQYWdlcxgZbXRvdGFsRWxlbWVudHMYGf8=';
const _episodeDetailCbor =
    'v2JpZHgkMDAwMDAwMDEtNTU1NS01NTU1LTU1NTUtNTU1NTU1NTU1NTU1Z2FuaW1lSWR4JDAwMDAwMDAxLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMWZzZWFzb24Ba2VwaXNvZGVUeXBlZ0VQSVNPREVmbnVtYmVyAW9yZWxlYXNlRGF0ZVRpbWVwMjAyNC0wMS0wN1QxMjowMGhkdXJhdGlvbhkFoGljcmVhdGVkQXRwMjAyNS0wMS0wMVQxMDowMGl1cGRhdGVkQXRwMjAyNS0wNi0wMVQxMjowMG1sb2NhbGl6YXRpb25zn79mbG9jYWxlZUZSX0ZSZXRpdGxlasOJcGlzb2RlIDFrZGVzY3JpcHRpb254GURlc2NyaXB0aW9uIG9mIGVwaXNvZGUgMS7/v2Zsb2NhbGVlRU5fVVNldGl0bGVpRXBpc29kZSAxa2Rlc2NyaXB0aW9ueBlEZXNjcmlwdGlvbiBvZiBlcGlzb2RlIDEu//9ocmVsZWFzZXOfv2hwbGF0Zm9ybWtDUlVOQ0hZUk9MTGpwbGF0Zm9ybUlkZDUwMTBjdXJseCZodHRwczovL3BsYXRmb3JtLmV4YW1wbGUuY29tL3dhdGNoLzEtMGthdWRpb0xvY2FsZWVGUl9GUmp1bmNlbnNvcmVk9G9yZWxlYXNlRGF0ZVRpbWVwMjAyNC0wMS0wN1QxMjowMP+/aHBsYXRmb3JteBlBTklNQVRJT05fRElHSVRBTF9ORVRXT1JLanBsYXRmb3JtSWRkNTAxMWN1cmx4Jmh0dHBzOi8vcGxhdGZvcm0uZXhhbXBsZS5jb20vd2F0Y2gvMS0xa2F1ZGlvTG9jYWxlZUpBX0pQanVuY2Vuc29yZWT1b3JlbGVhc2VEYXRlVGltZXAyMDI0LTAxLTA3VDEyOjAw//9maW1hZ2Vzn///';

List<int> _decode(String value) => base64Decode(value);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('settings', () {
    test('defaults to JSON when no preference is stored', () async {
      final viewModel = SettingsViewModel();
      await viewModel.load();

      expect(viewModel.format, ResponseFormat.json);
      expect(viewModel.isLoaded, isTrue);
    });

    test('persists the selected format across a new view model', () async {
      final first = SettingsViewModel();
      await first.load();
      await first.select(ResponseFormat.cbor);

      final second = SettingsViewModel();
      await second.load();

      expect(second.format, ResponseFormat.cbor);
    });

    test('falls back to JSON for an unknown stored value', () async {
      SharedPreferences.setMockInitialValues({'response_format': 'xml'});

      final viewModel = SettingsViewModel();
      await viewModel.load();

      expect(viewModel.format, ResponseFormat.json);
    });
  });

  group('protobuf decoder', () {
    test('decodes the anime page', () {
      final page = PagedResult.fromJson(
        decodeProtobuf(
          _bytes(_animesPageProtobuf),
          pageSchema(animeSummarySchema),
        ),
        AnimeSummaryModel.fromJson,
      );

      expect(page.data.single.title, "L'Attaque des Titans");
      expect(page.data.single.platforms, [
        'CRUNCHYROLL',
        'ANIMATION_DIGITAL_NETWORK',
      ]);
      expect(page.page, 1);
      expect(page.totalPages, 3);
      expect(page.totalElements, 3);
    });

    test('decodes the anime detail', () {
      final anime = AnimeDetailModel.fromJson(
        decodeProtobuf(_bytes(_animeDetailProtobuf), animeDetailSchema),
      );

      expect(
        anime.localizations.map((localization) => localization.title),
        contains('Le pirate au chapeau de paille'),
      );
      expect(anime.createdAt, DateTime(2025, 1, 1, 10));
      expect(anime.localizations, hasLength(2));
      expect(anime.platforms, hasLength(2));
      expect(anime.images.first.type, 'BANNER');
    });

    test('decodes the episode page', () {
      final page = PagedResult.fromJson(
        decodeProtobuf(
          _bytes(_episodesPageProtobuf),
          pageSchema(episodeSummarySchema),
        ),
        EpisodeSummaryModel.fromJson,
      );

      expect(page.data.single.title, 'Épisode 1');
      expect(page.data.single.releaseCount, 2);
      expect(page.totalPages, 25);
      expect(page.totalElements, 25);
    });

    test('decodes the episode detail', () {
      final episode = EpisodeDetailModel.fromJson(
        decodeProtobuf(_bytes(_episodeDetailProtobuf), episodeDetailSchema),
      );

      expect(episode.animeId, '00000001-1111-1111-1111-111111111111');
      expect(episode.localizations, hasLength(2));
      expect(episode.releases, hasLength(2));
      expect(episode.releases.last.audioLocale, 'JA_JP');
      expect(episode.releases.last.uncensored, isTrue);
    });
  });

  group('CBOR', () {
    test('decodes the anime page', () {
      final page = PagedResult.fromJson(
        normalizeCbor(cbor.decode(_decode(_animesPageCbor)))
            as Map<String, dynamic>,
        AnimeSummaryModel.fromJson,
      );

      expect(page.data.single.normalizedTitle, 'Attack on Titan');
      expect(page.totalElements, 3);
    });

    test('decodes the anime detail', () {
      final anime = AnimeDetailModel.fromJson(
        normalizeCbor(cbor.decode(_decode(_animeDetailCbor)))
            as Map<String, dynamic>,
      );

      expect(
        anime.localizations.map((localization) => localization.title),
        contains('Le pirate au chapeau de paille'),
      );
      expect(anime.images, hasLength(2));
      expect(anime.localizations, hasLength(2));
    });

    test('decodes the episode page', () {
      final page = PagedResult.fromJson(
        normalizeCbor(cbor.decode(_decode(_episodesPageCbor)))
            as Map<String, dynamic>,
        EpisodeSummaryModel.fromJson,
      );

      expect(page.data.single.title, 'Épisode 1');
      expect(page.data.single.releaseCount, 2);
      expect(page.totalElements, 25);
    });

    test('decodes the episode detail', () {
      final episode = EpisodeDetailModel.fromJson(
        normalizeCbor(cbor.decode(_decode(_episodeDetailCbor)))
            as Map<String, dynamic>,
      );

      expect(episode.animeId, '00000001-1111-1111-1111-111111111111');
      expect(episode.releases, hasLength(2));
      expect(episode.releases.last.audioLocale, 'JA_JP');
      expect(episode.releases.last.uncensored, isTrue);
    });
  });

  group('HTTP client', () {
    test('sends the negotiated Accept header and decodes JSON', () async {
      final settings = SettingsViewModel();
      await settings.load();
      final client = MockClient((request) async {
        expect(request.headers['Accept'], 'application/json');
        return http.Response(
          jsonEncode({
            'data': [],
            'page': 1,
            'size': 20,
            'totalPages': 0,
            'totalElements': 0,
          }),
          200,
        );
      });
      final httpClient = HttpClient(
        settings,
        client: client,
        baseUrl: 'http://localhost:8081',
      );

      final result = await httpClient.get<Map<String, dynamic>>('v1/animes');

      expect(result, isA<ApiSuccess<Map<String, dynamic>>>());
    });

    test('sends the negotiated Accept header and decodes protobuf', () async {
      final settings = SettingsViewModel();
      await settings.load();
      await settings.select(ResponseFormat.protobuf);
      final client = MockClient((request) async {
        expect(request.headers['Accept'], 'application/protobuf');
        return http.Response.bytes(
          _bytes(_animesPageProtobuf),
          200,
          headers: {'content-type': 'application/protobuf'},
        );
      });
      final httpClient = HttpClient(
        settings,
        client: client,
        baseUrl: 'http://localhost:8081',
      );

      final result = await httpClient.get<Map<String, dynamic>>(
        'v1/animes',
        protoSchema: pageSchema(animeSummarySchema),
      );

      expect(result, isA<ApiSuccess<Map<String, dynamic>>>());
    });

    test('sends the negotiated Accept header and decodes CBOR', () async {
      final settings = SettingsViewModel();
      await settings.load();
      await settings.select(ResponseFormat.cbor);
      final client = MockClient((request) async {
        expect(request.headers['Accept'], 'application/cbor');
        return http.Response.bytes(
          _decode(_animesPageCbor),
          200,
          headers: {'content-type': 'application/cbor'},
        );
      });
      final httpClient = HttpClient(
        settings,
        client: client,
        baseUrl: 'http://localhost:8081',
      );

      final result = await httpClient.get<Map<String, dynamic>>('v1/animes');

      expect(result, isA<ApiSuccess<Map<String, dynamic>>>());
    });
  });

  group('settings screen', () {
    testWidgets('selects and persists the CBOR format', (tester) async {
      final settings = SettingsViewModel();
      await settings.load();

      await tester.pumpWidget(
        ChangeNotifierProvider.value(
          value: settings,
          child: MaterialApp(
            localizationsDelegates: AppLocalizations.localizationsDelegates,
            supportedLocales: AppLocalizations.supportedLocales,
            home: const SettingsView(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Request format'), findsOneWidget);

      await tester.tap(find.text('CBOR'));
      await tester.pumpAndSettle();

      expect(settings.format, ResponseFormat.cbor);
      final preferences = await SharedPreferences.getInstance();
      expect(preferences.getString('response_format'), 'cbor');
    });
  });
}

Uint8List _bytes(String value) => Uint8List.fromList(_decode(value));
