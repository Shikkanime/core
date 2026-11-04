import 'package:flutter/foundation.dart';
import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/models/episode/episode_detail_model.dart';
import 'package:shikkanime_admin/repositories/episode_repository.dart';

/// State of the episode detail screen: the stored episode and its loading state.
class EpisodeDetailViewModel extends ChangeNotifier {
  EpisodeDetailViewModel(this._repository, this.episodeId);

  final EpisodeRepository _repository;
  final String episodeId;

  EpisodeDetailModel? _episode;
  bool _loading = false;
  String? _error;

  EpisodeDetailModel? get episode => _episode;

  bool get isLoading => _loading;

  String? get error => _error;

  /// Loads the episode displayed by the screen.
  Future<void> load() async {
    _loading = true;
    _error = null;
    notifyListeners();

    final result = await _repository.findById(episodeId);

    switch (result) {
      case ApiSuccess<EpisodeDetailModel>(:final data):
        _episode = data;
      case ApiFailure<EpisodeDetailModel>(:final error):
        _error = error;
    }

    _loading = false;
    notifyListeners();
  }
}
