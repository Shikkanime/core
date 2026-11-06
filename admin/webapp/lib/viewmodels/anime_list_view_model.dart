import 'dart:async';

import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/models/paged_result.dart';
import 'package:shikkanime_admin/repositories/anime_repository.dart';
import 'package:shikkanime_admin/viewmodels/paged_view_model.dart';

/// State of the catalog screen: the page displayed and the filters it is read with.
class AnimeListViewModel extends PagedViewModel<AnimeSummaryModel> {
  AnimeListViewModel(this._repository, {String? search, int page = 1})
    : _query = search?.trim() ?? '',
      super(initialPage: page);

  static const _searchDebounce = Duration(milliseconds: 300);

  final AnimeRepository _repository;

  Timer? _debounce;
  String _query;

  /// Rows of the catalog page currently displayed.
  List<AnimeSummaryModel> get animes => items;

  /// Term currently sent to the API, empty when the catalog is not filtered.
  String get query => _query;

  /// Sends the term to the API after a short debounce.
  ///
  /// The search runs in the database: filtering the loaded page in memory would answer a query
  /// the server answers with one indexed read, and would miss every row the page does not hold.
  void search(String value) {
    _query = value.trim();
    _debounce?.cancel();
    _debounce = Timer(_searchDebounce, () => load(page: 1));
    notifyListeners();
  }

  @override
  Future<ApiResult<PagedResult<AnimeSummaryModel>>> fetchPage(int page) {
    return _repository.findPage(
      page: page,
      size: pageSize,
      search: _query.isEmpty ? null : _query,
    );
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }
}
