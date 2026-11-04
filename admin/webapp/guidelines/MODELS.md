# Models, schemas and view models

Duplication is the thing this file removes: a field declared once, a page described once, a paging
mechanism written once.

## Models live by aggregate

```
lib/models/
  anime/
    anime_summary_model.dart
    anime_detail_model.dart
    anime_localization_model.dart
    anime_platform_model.dart
  episode/
    episode_summary_model.dart
    episode_detail_model.dart
    episode_localization_model.dart
    episode_release_model.dart
  image/
    image_model.dart
  paged_result.dart
  shikk_model.dart
```

- One aggregate per folder, named after the Kotlin aggregate it mirrors (`anime`, `episode`, ...).
  A type shared by several aggregates (`image`, the page envelope) stays at the root of `models/`.
- A file name is the type it declares: `anime_summary_model.dart` holds `AnimeSummaryModel` and
  nothing else. A page is never declared next to a row: it is `PagedResult<T>`.

## The shared fields, like the Kotlin data classes

The API repeats the audit columns on every row; the client declares them once.

```dart
/// Identity and audit columns shared by every aggregate the administration reads.
abstract interface class ShikkModel {
  String get id;
  DateTime? get createdAt;
  DateTime? get updatedAt;
}
```

- `AnimeDetailModel implements ShikkModel`, `EpisodeDetailModel implements ShikkModel`: the three
  columns are declared once, and a projection that does not carry them (a summary row, a release
  row) simply does not implement the interface. An interface rather than a base class: the
  generated `fromJson` of `json_serializable` only fills the fields it declares, so a shared
  constructor would silently drop `id` at decoding time.
- A page is one generic envelope, never one class per endpoint (`lib/models/paged_result.dart`):

```dart
/// One page of a paged collection, with the bounds the server returned.
class PagedResult<T> {
  const PagedResult({required this.data, required this.page, required this.size,
      required this.totalPages, required this.totalElements});

  factory PagedResult.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) parse,
  ) => ...;

  final List<T> data;
  final int page;
  final int size;
  final int totalPages;
  final int totalElements;
}
```

  `AnimePageModel` / `EpisodePageModel` / `LatestEpisodePageModel` do not exist: a repository
  decodes its answer into `PagedResult<AnimeSummaryModel>` with a single `PagedResult.fromJson(data,
  AnimeSummaryModel.fromJson)`, and a `PagedViewModel` consumes it as is.

## Protobuf schemas, one file per aggregate

`lib/core/network/schemas/` mirrors `models/`:

- `anime.dart` holds `animeSummarySchema`, `animeDetailSchema`, `animeLocalizationSchema`,
  `animePlatformSchema`;
- `episode.dart` holds the episode and release schemas;
- `image.dart`, `locale.dart` hold the shared enumerations;
- `page.dart` (schemas) holds **one** `pageSchema(ProtoSchema element)` builder, used by every paged endpoint:

```dart
ProtoSchema pageSchema(ProtoSchema element) => ProtoSchema({
  1: ProtoField('data', ProtoKind.message, repeated: true, message: element),
  2: ProtoField('page', ProtoKind.integer),
  3: ProtoField('size', ProtoKind.integer),
  4: ProtoField('totalPages', ProtoKind.integer),
  5: ProtoField('totalElements', ProtoKind.integer),
});
```

  A new endpoint declares its element schema and reuses `pageSchema`; the four pagination fields are
  written once, exactly like the Kotlin `PageDto`. The field numbers must match the declaration
  order of the Kotlin DTO, which is what Protobuf uses.

## Paged view models

A list screen repeats `page`, `totalPages`, `totalElements`, `loading`, `error`, the request token and
`goToPage`. That mechanism is a base class:

```dart
/// State of a paged list: the page displayed, its bounds and the guarded load.
abstract class PagedViewModel<T> extends ChangeNotifier {
  PagedViewModel({this.pageSize = 20});

  final int pageSize;
  final List<T> items = [];
  int page = 1;
  int totalPages = 0;
  int totalElements = 0;
  bool isLoading = false;
  String? error;

  /// Loads one page; subclasses implement the repository call.
  Future<ApiResult<PagedResult<T>>> fetchPage(int page);

  /// Loads the current page, or [page], and drops a superseded answer.
  Future<void> load({int? page});

  /// Loads a page selected in the paginator.
  void goToPage(int page);
}
```

- `AnimeListViewModel extends PagedViewModel<AnimeSummary>` implements `fetch` and adds `search`
  (debounced) and `platform`; `LatestEpisodesViewModel extends PagedViewModel<LatestEpisode>`
  implements `fetch` and adds nothing else.
- The base notifies once per load and owns the request token: a screen never re-implements the
  "stale answer" guard.
- A non-paged ViewModel (a detail screen) extends `ChangeNotifier` directly.
