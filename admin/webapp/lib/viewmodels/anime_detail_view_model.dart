import 'package:flutter/foundation.dart';
import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';
import 'package:shikkanime_admin/repositories/anime_repository.dart';
import 'package:shikkanime_admin/repositories/episode_repository.dart';

/// State of the anime detail screen: the anime and the page of its episodes.
class AnimeDetailViewModel extends ChangeNotifier {
  AnimeDetailViewModel(
    this._animeRepository,
    this._episodeRepository,
    this.animeId,
    this.initialTitle,
  );

  static const pageSize = 20;

  final AnimeRepository _animeRepository;
  final EpisodeRepository _episodeRepository;
  final String animeId;
  final String initialTitle;

  final _episodes = <EpisodeSummaryModel>[];

  AnimeDetailModel? _anime;
  int _animeRequestId = 0;
  int _episodeRequestId = 0;
  bool _animeLoading = false;
  bool _episodesLoading = false;
  String? _animeError;
  String? _episodesError;
  int _page = 1;
  int _totalPages = 0;
  int _totalElements = 0;

  AnimeDetailModel? get anime => _anime;

  String get title => _anime?.normalizedTitle ?? initialTitle;

  List<EpisodeSummaryModel> get episodes => _episodes;

  bool get isAnimeLoading => _animeLoading;

  bool get isEpisodesLoading => _episodesLoading;

  String? get animeError => _animeError;

  String? get episodesError => _episodesError;

  int get page => _page;

  int get totalPages => _totalPages;

  int get totalElements => _totalElements;

  bool get canPrevious => _page > 1;

  bool get canNext => _page < _totalPages;

  /// Loads the full anime and the first page of its episodes.
  Future<void> load() async {
    await Future.wait([_loadAnime(), loadEpisodes(1)]);
  }

  Future<void> _loadAnime() async {
    final requestId = ++_animeRequestId;

    _animeLoading = true;
    _animeError = null;
    notifyListeners();

    final result = await _animeRepository.findById(animeId);

    if (requestId != _animeRequestId) return;

    switch (result) {
      case ApiSuccess<AnimeDetailModel>(:final data):
        _anime = data;
      case ApiFailure<AnimeDetailModel>(:final error):
        _animeError = error;
    }

    _animeLoading = false;
    notifyListeners();
  }

  /// Loads one page of the episodes of the anime.
  Future<void> loadEpisodes(int page) async {
    final requestId = ++_episodeRequestId;

    _episodesLoading = true;
    _episodesError = null;
    notifyListeners();

    final result = await _episodeRepository.findPageByAnimeId(
      animeId,
      page: page,
      size: pageSize,
    );

    if (requestId != _episodeRequestId) return;

    switch (result) {
      case ApiSuccess<PagedResult<EpisodeSummaryModel>>(:final data):
        _episodes
          ..clear()
          ..addAll(data.data);
        _page = data.page;
        _totalPages = data.totalPages;
        _totalElements = data.totalElements;
      case ApiFailure<PagedResult<EpisodeSummaryModel>>(:final error):
        _episodesError = error;
    }

    _episodesLoading = false;
    notifyListeners();
  }

  /// Loads a page selected in the episode paginator.
  void goToPage(int page) {
    final target = page.clamp(1, _totalPages == 0 ? 1 : _totalPages);

    if (target == _page && !_episodesLoading) return;

    loadEpisodes(target);
  }
}
