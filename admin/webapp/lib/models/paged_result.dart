/// One page of a paged collection, with the bounds the server returned.
///
/// It is the client counterpart of the Kotlin `Page`: the four bounds travel with the rows, so a
/// screen renders its paginator from one answer and never counts a collection it did not read.
class PagedResult<T> {
  const PagedResult({
    required this.data,
    required this.page,
    required this.size,
    required this.totalPages,
    required this.totalElements,
  });

  /// Rows of the page.
  final List<T> data;

  /// One-based index of this page.
  final int page;

  /// Maximum number of rows a page may hold.
  final int size;

  /// Number of pages the query spans.
  final int totalPages;

  /// Number of rows matching the query, all pages included.
  final int totalElements;

  /// Reads the page envelope the API returns for every paged endpoint.
  ///
  /// [parse] converts one row; the four bounds are decoded here once, so an endpoint never
  /// repeats them.
  factory PagedResult.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) parse,
  ) => PagedResult(
    data: (json['data'] as List<dynamic>)
        .map((row) => parse(row as Map<String, dynamic>))
        .toList(),
    page: json['page'] as int,
    size: json['size'] as int,
    totalPages: json['totalPages'] as int,
    totalElements: json['totalElements'] as int,
  );
}
