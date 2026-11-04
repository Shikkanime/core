# LLM Agent Instructions - Shikkanime Administration (Flutter web)

The administration is a Flutter **web** application living inside the `:admin` module of `core`. It
follows the architecture of the `Shikkanime/application` v2 app — **MVVM**, `provider`,
`ChangeNotifier`, immutable DTOs — and it is served by the admin server itself: the compiled build
is packaged under the `webapp` resource of the module, so starting the admin serves both the
interface and its JSON API.

These rules are non-negotiable. For the shared conventions (full API mapping pattern, testing,
localization) the reference is the `Shikkanime/application` v2 `guidelines/` directory; the documents
here record what is specific to the administration.

## 1. Respect the MVVM layers

```
View (widgets) -> ViewModel (ChangeNotifier) -> Repository -> HttpClient -> Admin JSON API
```

- **Views** (`lib/views/`) are dumb: they render the ViewModel state and forward events. The only
  logic allowed is a flag check, a layout decision and a route push.
- **ViewModels** (`lib/viewmodels/`) hold the screen state and orchestrate the repositories. They
  extend `ChangeNotifier` and call `notifyListeners()` after every mutation the UI observes.
- **Repositories** (`lib/repositories/`) are `const` classes taking an `HttpClient`; they build the
  endpoint, call it and map the payload to models. They are the only layer that talks HTTP.
- **Models** (`lib/models/`) are immutable DTOs generated with `json_serializable`
  (`@JsonSerializable(createToJson: false)` + `part`), with a hand-written getter only when the
  display needs one (`displayTitle`, `thumbnail`).

State is read with `context.watch<T>()` and events are sent with `context.read<T>()`. No global
singleton, no `setState` in a screen that has a ViewModel.

## 2. One origin, one API

- The interface calls the API of the **same server** (`EnvConfig.apiBaseUrl` falls back to
  `Uri.base.origin`). `--dart-define=API_BASE_URL=http://localhost:8081` targets another origin,
  which is what `flutter run` needs.
- Endpoints are versioned (`v1/animes`, `v1/episodes?animeId=…`). They are the admin controllers of
  `:admin` — never the `:api` module, which is a separate deployment.
- A collection endpoint is **paged**: `page` starts at 1, `size` is bounded by the server (20 by
  default, 100 at most), and the response repeats `page`, `size`, `totalPages` and `totalElements`.
  The client renders its paginator from that answer and never asks for a whole table.
- The payload shape is split between the list and the detail: `AdminAnimeSummaryDto` /
  `AdminEpisodeSummaryDto` carry only what a row displays, while `AdminAnimeDetailDto` /
  `AdminEpisodeDetailDto` carry the full row, its translations, releases and artwork URLs. They are
  the admin DTOs; the contract lives in the Kotlin DTOs, so the client changes when they change.
- Artwork is **not rendered**: the image host does not allow the administration origin (CORS). The
  detail screens list the artwork URLs as selectable text, and the list DTOs carry no image at all.
- Every request carries the `Accept` header of the format selected in **Settings**: JSON (default),
  Protobuf (`application/protobuf`) or CBOR (`application/cbor`). The choice is persisted in
  `shared_preferences`. JSON is decoded with `jsonDecode`, CBOR with the `cbor` package, and
  Protobuf with the schema of `lib/core/network/admin_protobuf_schemas.dart`; adding an endpoint in
  Protobuf means adding its schema there.

## 3. Write code for humans (and other AIs)

- Code, comments and documentation in **English**, and so is the interface: `lib/l10n/app_en.arb`
  is the template, and every user-visible string goes through `AppLocalizations`, never hardcoded in
  a widget. A French translation is a second `.arb` file, never a second set of strings in code.
- `const` constructors everywhere the analyzer allows them; one responsibility per class and
  function; no dead or commented-out code; no premature abstraction.
- Reusable widgets go to `lib/core/widgets/`; a widget used by one screen stays in
  `lib/views/<screen>/widgets/`.
- Widgets follow [CODE_STYLE.md](guidelines/CODE_STYLE.md): one widget per file, `StatelessWidget`
  unless the widget owns a controller or a timer, a `build` under 200-250 lines, skeletons instead of
  `CircularProgressIndicator`, and a server enum translated by an ARB `select`, never a Dart `switch`.
- Models, Protobuf schemas and view models follow [MODELS.md](guidelines/MODELS.md): one folder per
  aggregate, `id`/`createdAt`/`updatedAt` declared once in a shared base, one generic page envelope,
  one schema file per aggregate, and paged view models over a common base.
- Every public class, method and property carries a Dart doc comment, in English.

## 4. Search is a database concern

- Every filter the interface offers - a title, an identifier, anything - travels to the API as a
  query parameter and is executed as **SQL** by the repository. The client never filters a list it
  has already loaded: that answers a query the database can answer with one indexed read, and it
  silently misses every row the list does not hold yet.
- The search box is therefore debounced and calls the repository again; it does not narrow a local
  copy of the previous result.

## 5. Security and data

- All API input is untrusted: parse it at the repository boundary, tolerate a `null` title or an
  unknown enum value instead of crashing the screen.
- Enum values travel as Kotlin names (`ANIMATION_DIGITAL_NETWORK`, `FR_FR`). The admin displays them
  through the label helpers (`lib/core/format/labels.dart`), never by parsing them into a closed
  Dart enum: a new platform on the server must keep rendering.
- Never log a token, a password or a personal identifier.

## 6. Before submitting

- `dart format lib` and `flutter analyze` pass with **no** issue (`flutter analyze` is a CI job).
- `flutter test` passes, including the captured JSON/Protobuf/CBOR responses.
- `flutter build web --release` succeeds.
- The screen was opened against a running admin server and shows real data — a green analyzer says
  nothing about a screen that renders nothing.
- Run `./gradlew :admin:run` (with Flutter in `PATH` or `FLUTTER_BIN` set) to check the packaged
  build, not only the dev server.

See [ARCHITECTURE.md](guidelines/ARCHITECTURE.md) for the folder layout and the recipe to add a
screen, [CODE_STYLE.md](guidelines/CODE_STYLE.md) for widgets and documentation,
[MODELS.md](guidelines/MODELS.md) for models, schemas and view models, and
[API_CONVENTIONS.md](guidelines/API_CONVENTIONS.md) for the HTTP contract.

A review comment that reveals a convention is written in these guides in the same change, so the
next contributor reads the rule instead of rediscovering it.
