import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/core/network/http_client.dart';
import 'package:shikkanime_admin/core/network/schemas.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';

/// Reads the stored anime catalog.
class AnimeRepository {
  const AnimeRepository(this._httpClient);

  final HttpClient _httpClient;

  /// Loads one page of the catalog, or of its matches for a search term.
  ///
  /// The term travels to the API with the page bounds: the database filters and paginates, this
  /// client never reads a whole collection to narrow it.
  Future<ApiResult<PagedResult<AnimeSummaryModel>>> findPage({
    int page = 1,
    int size = 20,
    String? search,
    String? platform,
  }) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/animes',
      protoSchema: pageSchema(animeSummarySchema),
      queryParameters: {
        'page': page,
        'size': size,
        if (search != null && search.isNotEmpty) 'search': search,
        if (platform != null && platform.isNotEmpty) 'platform': platform,
      },
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) => ApiSuccess(
        PagedResult.fromJson(data, AnimeSummaryModel.fromJson),
      ),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure(
        failure.error,
        failure.statusCode,
      ),
    };
  }

  /// Loads everything the database knows about one anime.
  Future<ApiResult<AnimeDetailModel>> findById(String id) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/animes/$id',
      protoSchema: animeDetailSchema,
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) =>
        ApiSuccess<AnimeDetailModel>(AnimeDetailModel.fromJson(data)),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure<AnimeDetailModel>(
        failure.error,
        failure.statusCode,
      ),
    };
  }
}
