import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
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
  });
}
