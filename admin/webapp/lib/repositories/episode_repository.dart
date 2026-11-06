import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/core/network/http_client.dart';
import 'package:shikkanime_admin/core/network/schemas.dart';
import 'package:shikkanime_admin/models/episode/episode_detail_model.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';
import 'package:shikkanime_admin/models/episode/latest_episode_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';

/// Reads the stored episodes of an anime.
class EpisodeRepository {
  const EpisodeRepository(this._httpClient);

  final HttpClient _httpClient;

  /// Loads one page of the episodes of an anime.
  Future<ApiResult<PagedResult<EpisodeSummaryModel>>> findPageByAnimeId(
    String animeId, {
    int page = 1,
    int size = 20,
  }) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/episodes',
      protoSchema: pageSchema(episodeSummarySchema),
      queryParameters: {'animeId': animeId, 'page': page, 'size': size},
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) => ApiSuccess(
        PagedResult.fromJson(data, EpisodeSummaryModel.fromJson),
      ),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure(
        failure.error,
        failure.statusCode,
      ),
    };
  }

  /// Loads one page of every episode, newest release first.
  ///
  /// The order is the broadcast date of the episode, not the date of its platform releases.
  Future<ApiResult<PagedResult<LatestEpisodeModel>>> findLatest({
    int page = 1,
    int size = 20,
  }) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/episodes/latest',
      protoSchema: pageSchema(latestEpisodeSchema),
      queryParameters: {'page': page, 'size': size},
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) => ApiSuccess(
        PagedResult.fromJson(data, LatestEpisodeModel.fromJson),
      ),
      ApiFailure<Map<String, dynamic>> failure => ApiFailure(
        failure.error,
        failure.statusCode,
      ),
    };
  }

  /// Loads everything the database knows about one episode.
  Future<ApiResult<EpisodeDetailModel>> findById(String id) async {
    final response = await _httpClient.get<Map<String, dynamic>>(
      'v1/episodes/$id',
      protoSchema: episodeDetailSchema,
    );

    return switch (response) {
      ApiSuccess<Map<String, dynamic>>(:final data) =>
        ApiSuccess<EpisodeDetailModel>(EpisodeDetailModel.fromJson(data)),
      ApiFailure<Map<String, dynamic>> failure =>
        ApiFailure<EpisodeDetailModel>(failure.error, failure.statusCode),
    };
  }
}
