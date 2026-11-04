# Architecture

The administration is a thin read client over the admin JSON API: one screen per route of data, one
ViewModel per screen, one repository per resource, one URL per screen.

## Layout

```
lib/
  main.dart                     # Composition root: MultiProvider + MaterialApp.router
  router/app_router.dart        # go_router routes and their URLs
  core/
    config/env_config.dart      # API base URL (same origin by default)
    network/                    # HttpClient, ApiResult<T>, JSON/CBOR/Protobuf decoders
    network/schemas/            # Protobuf schemas, one file per aggregate
    settings/                   # ResponseFormat selected in Settings
    format/                     # Date and duration formatters
    theme/                      # Material 3 theme
    widgets/                    # Shared, reusable widgets (state views, skeleton, page)
  l10n/                         # app_en.arb + generated AppLocalizations
  models/                       # One folder per aggregate (anime/, episode/, ...) + .g.dart
  repositories/                 # const repositories over HttpClient
  viewmodels/                   # ChangeNotifier view models, paged base included
  views/                        # One folder per screen, one widget per file
```

## Adding a screen

1. **Model**: `lib/models/<aggregate>/<name>_model.dart` with
   `@JsonSerializable(createToJson: false)`, its `part` file and
   `dart run build_runner build --delete-conflicting-outputs`. A model extends the shared base types
   of [MODELS.md](MODELS.md) instead of redeclaring `id`, `createdAt`, `updatedAt` or the pagination
   bounds.
2. **Repository**: a method on the resource repository that builds the endpoint and maps the raw
   `Map<String, dynamic>` to the model inside a `switch` over `ApiResult`.
3. **ViewModel**: a `ChangeNotifier` holding the state. A paged list extends `PagedViewModel<T>` so
   `page`, `totalPages`, `totalElements`, `goToPage` and the request token live once, not per screen.
4. **View**: a `StatelessWidget` in `lib/views/<screen>/` reading `context.watch<T>()`. Its `build`
   stays under 200-250 lines; beyond that, extract the blocks into their own widgets, one per file.
5. **Route**: declare the path in `lib/router/app_router.dart` and the sidebar entry if the screen
   belongs to the main navigation.

## Navigation

`go_router` owns the URL: `/animes`, `/animes/<id>`, `/animes/<id>/episodes/<id>`,
`/platforms/<platform>`, `/episodes`, `/settings`. Screens navigate with `context.go(...)`, never
`Navigator.push`, so the browser history, a reload and a shared link reach the same view. The list
screens read their filters from the query string; the detail screens take their identifier from the
path and use `extra` only as a placeholder title while the data loads.

## Pagination

A list screen never holds a whole collection. `PagedViewModel<T>` keeps the current `page`, the
`totalPages` and the `totalElements` returned by the API, exposes `load({page})` and `goToPage(page)`,
and drops the answer of a request superseded by a newer one. A screen only adds what is specific to
it: the endpoint it calls and its filters.

## Settings

`SettingsViewModel` is application-scoped: it loads the persisted `ResponseFormat` before `runApp`,
so the first request already uses the stored format, and `HttpClient` reads it on every request.
`SettingsView` changes it and persists it through `shared_preferences`; opening a list again reloads
it with the new format.

## State rules

- A screen loads once when it opens (`addPostFrameCallback` in `initState`) and on explicit refresh.
- `load()` sets `loading`, clears `error`, calls the repository, then sets the result and notifies
  **once** at the end.
- A request carries a token: when a search or a page change starts a second request, the answer of
  the first one is dropped instead of overwriting the newer state.
- The ViewModel exposes read-only getters; the view never mutates a list it received.
- A `StatefulWidget` is kept for what only it can do - a `TextEditingController`, a scroll
  controller, a `TabController`, a subscription. Everything else is a `StatelessWidget`.
