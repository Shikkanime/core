import 'package:flutter/foundation.dart';
import 'package:shikkanime_admin/core/network/api_result.dart';
import 'package:shikkanime_admin/models/paged_result.dart';

/// State shared by every paged list: the page displayed, its bounds and the guarded load.
///
/// A screen only implements [fetchPage] - the endpoint it calls and the filters it sends - and
/// reads [items], [page], [totalPages] and [total] like any other state. The request token lives
/// here once: an answer superseded by a newer request is dropped instead of overwriting the state.
abstract class PagedViewModel<T> extends ChangeNotifier {
  PagedViewModel({this.pageSize = 20, int initialPage = 1})
    : _page = initialPage;

  /// Number of rows requested per page.
  final int pageSize;

  /// Rows of the page currently displayed.
  final List<T> items = [];

  int _requestId = 0;
  bool _loading = false;
  String? _error;
  int _page;
  int _totalPages = 0;
  int _totalElements = 0;

  /// Whether a request is in flight.
  bool get isLoading => _loading;

  /// Message of the last failure, or `null`.
  String? get error => _error;

  /// One-based index of the page displayed.
  int get page => _page;

  /// Number of pages the query spans.
  int get totalPages => _totalPages;

  /// Number of rows matching the query, all pages included.
  int get total => _totalElements;

  /// Calls the endpoint of the screen and maps its answer to a [PagedResult].
  ///
  /// Each screen implements this: the base owns the loading flag, the error and the stale-answer
  /// guard, so a list never re-implements them.
  ///
  /// [page] is the one-based page to request.
  Future<ApiResult<PagedResult<T>>> fetchPage(int page);

  /// Loads the current page, or [page] when the paginator asks for another one.
  Future<void> load({int? page}) async {
    final requestedPage = page ?? _page;
    final requestId = ++_requestId;

    _loading = true;
    _error = null;
    notifyListeners();

    final result = await fetchPage(requestedPage);

    if (requestId != _requestId) return;

    switch (result) {
      case ApiSuccess<PagedResult<T>>(:final data):
        items
          ..clear()
          ..addAll(data.data);
        _page = data.page;
        _totalPages = data.totalPages;
        _totalElements = data.totalElements;
      case ApiFailure<PagedResult<T>>(:final error):
        _error = error;
    }

    _loading = false;
    notifyListeners();
  }

  /// Loads a page selected in the paginator.
  void goToPage(int page) {
    final target = page.clamp(1, _totalPages == 0 ? 1 : _totalPages);

    if (target == _page && !_loading) return;

    load(page: target);
  }
}
