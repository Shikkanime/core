import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/core/network/http_client.dart';
import 'package:shikkanime_admin/core/network/schemas.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
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
  }) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/animes',
      protoSchema: pageSchema(animeSummaryModelProtoSchema),
      queryParameters: {
        'page': page,
        'size': size,
        if (search != null && search.isNotEmpty) 'search': search,
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
      protoSchema: animeDetailModelProtoSchema,
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

  /// Overrides the artwork of one type of an anime and marks it as manually managed.
  ///
  /// A manual image is never replaced by a platform harvest until [resetImage] releases it.
  Future<ApiResult<ImageModel>> updateImage(
    String animeId,
    String type,
    String url,
  ) async {
    final response = await _httpClient.patch<Map<String, dynamic>>(
      'v1/animes/$animeId/images/$type',
      body: {'url': url},
      protoSchema: imageModelProtoSchema,
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) => ApiSuccess<ImageModel>(
        ImageModel.fromJson(data),
      ),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure<ImageModel>(
        failure.error,
        failure.statusCode,
      ),
    };
  }

  /// Releases the manual management of one artwork of an anime.
  ///
  /// The current URL is kept until a platform harvest replaces it.
  Future<ApiResult<ImageModel>> resetImage(String animeId, String type) async {
    final response = await _httpClient.post<Map<String, dynamic>>(
      'v1/animes/$animeId/images/$type/reset',
      protoSchema: imageModelProtoSchema,
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) => ApiSuccess<ImageModel>(
        ImageModel.fromJson(data),
      ),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure<ImageModel>(
        failure.error,
        failure.statusCode,
      ),
    };
  }
}
