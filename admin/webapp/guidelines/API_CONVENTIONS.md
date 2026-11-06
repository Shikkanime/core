# API Conventions

One `HttpClient`, one sealed `ApiResult<T>`, one mapping point per repository. The contract is the
admin DTOs of the `:admin` module: when they change, this app changes in the same commit.

## ApiResult

```dart
sealed class ApiResult<T> {
  const ApiResult();
}

class ApiSuccess<T> extends ApiResult<T> {
  const ApiSuccess(this.data);
  final T data;
}

class ApiFailure<T> extends ApiResult<T> {
  const ApiFailure([this.error, this.statusCode]);
  final String? error;
  final int? statusCode;
}
```

Consumers handle it with an exhaustive `switch`, so a new case is a compile error, not a silent
`null`.

## HttpClient

`get<T>(endpoint, queryParameters: {...})` builds the URI from `EnvConfig.apiBaseUrl`, applies the
timeout, rejects a non-2xx status and decodes JSON into the **raw** type the caller asks for:
`Map<String, dynamic>` for one object, `List<dynamic>` for a collection. `T` is never a model.

## Repository mapping

```dart
Future<ApiResult<AnimePageModel>> findPage({int page = 1, int size = 20}) async {
  final response = await _httpClient.get<Map<String, dynamic>>(
    'v1/animes',
    queryParameters: {'page': page, 'size': size},
  );

  return switch (response) {
    ApiSuccess<Map<String, dynamic>>(:final data) =>
      ApiSuccess(AnimePageModel.fromJson(data)),
    ApiFailure<Map<String, dynamic>> failure => ApiFailure(
      failure.error,
      failure.statusCode,
    ),
  };
}
```

- Query parameters are added conditionally (collection-`if`), never as empty strings.
- A repository method returns the domain result with the typed failure carried over: the ViewModel
  decides what to display, the repository never formats.

## Endpoints consumed today

- `GET /v1/animes?page=<n>&size=<n>&search=<term>` → `PageDto<AdminAnimeSummaryDto>`: one page of
  the catalog, with its `page`, `size`, `totalPages` and `totalElements`. Without `search` the
  catalog is paged; with it, the database returns the page of matches on the identity title, the
  localized titles and the identifier.
- `GET /v1/animes/{id}` → `AdminAnimeDetailDto`: the full anime — audit columns, translations with
  their descriptions, platform links and artwork URLs.
- `GET /v1/episodes?animeId=<uuid>&page=<n>&size=<n>` → `PageDto<AdminEpisodeSummaryDto>`: one page
  of the episodes of one anime, in broadcast order.
- `GET /v1/episodes/{id}` → `AdminEpisodeDetailDto`: the full episode — audit columns, translations,
  platform releases and artwork URLs.

## Pagination

A collection is never returned whole. The repository sends `page` (one-based) and `size`, and maps
the `data`, `page`, `size`, `totalPages` and `totalElements` fields of the answer into its page
model. The ViewModel keeps the bounds and exposes `goToPage`, `canPrevious` and `canNext`; the
`PaginationBar` renders them. A search term resets the page to 1 before calling the API.

The payload stays compact by construction: the list DTOs only carry the fields a row displays, and
the descriptions, releases and artwork URLs are fetched with the detail DTO when a row is opened.

## Artwork

The administration does not display images: the image host does not allow the page origin (CORS).
The detail screens list the artwork URLs as selectable text, and the list DTOs carry no image field
at all. Adding an `Image.network` back would fail at runtime, not at build time.

## Wire formats

The client selects JSON, Protobuf or CBOR in the Settings screen; the default is JSON and the choice
is stored in `shared_preferences`. `HttpClient` sends the matching `Accept` header and decodes to the
same JSON-like `Map<String, dynamic>` shape, so repositories and models do not know the transport.

- JSON: `application/json`, decoded with `jsonDecode`.
- Protobuf: `application/protobuf`, decoded with `decodeProtobuf` and the schema of
  `lib/core/network/admin_protobuf_schemas.dart`. Kotlinx serialization numbers fields in declaration
  order; a DTO field added or reordered on the server must update the schema in the same change.
- CBOR: `application/cbor`, decoded with the `cbor` package and normalized to string keys.

`flutter test test/response_formats_test.dart` decodes captured responses of the three formats for
the four admin endpoints. A new endpoint needs its schema and a fixture in that test.

## Search

A search term is a query parameter, never a client-side filter: the repository builds the SQL
predicate (case-insensitive `LIKE` on the identity and localized titles, identifier equality) and
the client sends the term again on each debounced keystroke. Filtering in memory would load the
whole catalog to answer one query and would only see the rows already fetched.

## Enum values

The server serializes Kotlin enum names (`ANIMATION_DIGITAL_NETWORK`, `FR_FR`, `SPIN_OFF`). They are
read as `String` and displayed through `lib/core/format/labels.dart`: an enum entry added on the
server keeps rendering instead of failing a `byName` lookup.
