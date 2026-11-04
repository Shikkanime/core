import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/models/episode/latest_episode_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';
import 'package:shikkanime_admin/repositories/episode_repository.dart';
import 'package:shikkanime_admin/viewmodels/paged_view_model.dart';

/// State of the episode list: the page displayed, newest release first.
class LatestEpisodesViewModel extends PagedViewModel<LatestEpisodeModel> {
  LatestEpisodesViewModel(this._repository);

  final EpisodeRepository _repository;

  /// Rows of the release list page currently displayed.
  List<LatestEpisodeModel> get episodes => items;

  @override
  Future<ApiResult<PagedResult<LatestEpisodeModel>>> fetchPage(int page) {
    return _repository.findLatest(page: page, size: pageSize);
  }
}
