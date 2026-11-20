import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/models/metadata/metadata_link_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';

void main() {
  test('parses one page of the catalog with its pagination bounds', () {
    final page = PagedResult.fromJson({
      'data': [
        {
          'id': '00000001-1111-1111-1111-111111111111',
          'title': 'Le pirate au chapeau de paille',
          'normalizedTitle': 'One Piece',
          'platforms': ['CRUNCHYROLL', 'ANIMATION_DIGITAL_NETWORK'],
          'localizationCount': 2,
        },
      ],
      'page': 2,
      'size': 1,
      'totalPages': 3,
      'totalElements': 3,
    }, AnimeSummaryModel.fromJson);

    expect(page.data.single.id, isNotEmpty);
    expect(page.data.single.title, 'Le pirate au chapeau de paille');
    expect(page.page, 2);
    expect(page.size, 1);
    expect(page.totalPages, 3);
    expect(page.totalElements, 3);
    expect(page.data.single.platforms, hasLength(2));
  });

  test('parses the full anime detail', () {
    final anime = AnimeDetailModel.fromJson({
      'id': '00000001-1111-1111-1111-111111111111',
      'normalizedTitle': 'One Piece',
      'createdAt': '2025-01-01T10:00',
      'updatedAt': null,
      'localizations': [
        {
          'locale': 'FR_FR',
          'title': 'Le pirate au chapeau de paille',
          'description': 'Gomu Gomu no Mi',
        },
      ],
      'platforms': [
        {'platform': 'CRUNCHYROLL', 'platformId': '1000'},
      ],
      'images': [
        {'type': 'BANNER', 'url': 'https://cdn.example.com/banner.jpg'},
      ],
    });

    expect(anime.createdAt, DateTime(2025, 1, 1, 10));
    expect(anime.updatedAt, isNull);
    expect(anime.localizations.single.description, 'Gomu Gomu no Mi');
    expect(anime.images.single.type, 'BANNER');
    expect(anime.metadataLinks, isEmpty);
  });

  test('parses metadata links and builds their platform URLs', () {
    final anime = AnimeDetailModel.fromJson({
      'id': 'anime-id',
      'normalizedTitle': 'One Piece',
      'localizations': <dynamic>[],
      'platforms': <dynamic>[],
      'images': <dynamic>[],
      'metadataLinks': [
        {'platform': 'ANILIST', 'platformId': '21'},
        {'platform': 'ANIDB', 'platformId': '69'},
      ],
    });

    expect(anime.metadataLinks, hasLength(2));
    expect(anime.metadataLinks.first.url, 'https://anilist.co/anime/21');
    expect(anime.metadataLinks.last.url, 'https://anidb.net/anime/69');
  });

  test('builds episode page URLs for metadata links', () {
    MetadataLinkModel link(
      String platform,
      String platformId,
      String? animeId,
    ) => MetadataLinkModel(platform, platformId, animeId);

    // MyAnimeList platformId is the composite "{animeId}/{episodeNumber}".
    expect(
      link('MYANIMELIST', '7655/15', '7655').url,
      'https://myanimelist.net/anime/7655/15',
    );
    // AniDB platformId is the episode eid, resolved with its parent anime aid.
    expect(
      link('ANIDB', '118925', '7236').url,
      'https://anidb.net/perl-bin/animedb.pl?show=ep&aid=7236&eid=118925',
    );
    // AniList has no episode entries: the parent anime page is the fallback.
    expect(
      link('ANILIST', '7655', '7655').url,
      'https://anilist.co/anime/7655',
    );
    // An unparseable MyAnimeList identifier falls back to the anime page too.
    expect(
      link('MYANIMELIST', '7655', '7655').url,
      'https://myanimelist.net/anime/7655',
    );
  });
}
