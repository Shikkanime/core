# Implementing a Streaming Platform

This guide explains how to add a new streaming platform to the harvest pipeline, following the
same conventions as the existing connectors (`CrunchyrollPlatform`,
`AnimationDigitalNetworkPlatform`, `PrimeVideoPlatform`).

## 1. The contract

Every platform is a class in `jobs/src/main/kotlin/platforms/impl/` implementing
`StreamingPlatform` (`jobs/src/main/kotlin/platforms/StreamingPlatform.kt`):

```kotlin
interface StreamingPlatform {
    val platform: Platform
    val countries: Set<Country>
    suspend fun fetchLatestEpisodes(): SmartResponse<List<PlatformEpisode>>
}
```

- `platform` identifies the connector; add a value to `Platform` in `:models` if the platform
  does not exist yet (`models/src/main/kotlin/Platform.kt`). The enum order matters: it drives
  the metadata priority used by `HarvestIngestionService`.
- `countries` declares the distributions the connector harvests (e.g. `setOf(Country.FR)`).
  Most connectors iterate over this set and produce one harvest per country.
- `fetchLatestEpisodes()` returns a `SmartResponse<List<PlatformEpisode>>`: the normalized
  episodes of every configured country plus a `hasChanged` flag.

## 2. Normalized models

The harvest must be expressed with the models of `jobs/src/main/kotlin/platforms/models/`:

- `PlatformAnime`: platform identifier (`id`), `PlatformImage` list (`BANNER`, `THUMBNAIL`,
  `TITLE`), and `PlatformAnimeLocalization` entries carrying `country`, `locale`, `title` and
  `description`.
- `PlatformEpisode`: parent `PlatformAnime`, stable platform `id`, `image`,
  `releaseDateTime` (`ZonedDateTime`), `season`, `number`, `episodeType`
  (`EPISODE`, `FILM`, `SPECIAL`, `SUMMARY`, `SPIN_OFF`), `duration` in seconds, watch `url`,
  `audioLocales`, `uncensored`, and `PlatformEpisodeLocalization` entries.
- `PlatformEpisode.toModel()` / `toReleaseModels()` map the harvest to the persisted
  `EpisodeWithChildren`: one `EpisodeRelease` row is created per distinct audio locale, so a
  subtitled-only release still keeps its original audio track.

Run every harvested text through `sanitizeText()` (`platforms/TextSanitizer.kt`) before storing
it in a localization: it strips line breaks and non-breaking spaces and turns blank text into
`null`.

## 3. Registration

Annotate the connector with Koin and bind it to the interface:

```kotlin
@Single(binds = [StreamingPlatform::class])
class MyPlatform(
    private val client: SmartHttpClient,
    ...
) : StreamingPlatform
```

`SmartHttpClient` is itself a `@Single`, so constructor injection is enough — no module file to
edit. `FetchLatestEpisodesJob` injects `List<StreamingPlatform>` and picks every registered
connector automatically. Constructor parameters with defaults (countries, catalog identifiers)
can be overridden by tests or future configuration.

## 4. HTTP layer

Use `SmartHttpClient` for every request. It provides ETag/`Last-Modified`/body-hash change
detection, single-flight coalescing, an LRU memory cache with stale fallback, and response body
archiving under `logs/http-responses`. The result is a `SmartResponse<T>` whose `hasChanged`
flag tells whether the remote payload actually changed since the last fetch:

```kotlin
client.get<PrimeVideoPageResponse>(
    url = ...,
    cacheKey = "my_platform:detail:$id",
    ttl = ttl,
    headers = mapOf(...)
)
```

Always pass a deterministic `cacheKey` scoped by language/region when the same URL serves
different payloads per country.

**Bot checks and redirect loops.** Some endpoints answer the shared Ktor client with a
redirect loop that exhausts `HttpSend.maxSendCount` once their bot check challenges the
request. When headers alone do not fix it, give the endpoint a dedicated
`SmartHttpClient` wrapping a Ktor `HttpClient(OkHttp)` built like `createHttpClient()` but
with `followRedirects = false` — `PrimeVideoPlatform.widgetClient` shows the pattern. This
keeps caching, change detection and stale fallback while the redirect-free client negotiates
the JSON payload. Keep the bypass minimal and document why.

## 5. Harvesting strategy

There is no universal scraping recipe; pick the cheapest reliable source:

- **Public JSON endpoints** negotiated with query parameters and `Accept`/`x-requested-with`
  headers are preferable to parsing HTML.
- **Authenticated mobile APIs** usually require service tokens (see the Prime Video APK
  findings: `/cdp/discovery/GetBrowsePage` answers `400` without one) — prefer the public web
  surface when it exposes the same data.
- **Catalog discovery**: if the platform has no public "latest episodes" feed for the tracked
  catalog, carry the known identifiers as a default set in the connector (Prime Video does
  this with `DEFAULT_SHOW_IDS`, migrated from the production database).

**Image normalization.** `ImageType` declares canonical dimensions (`THUMBNAIL` 1560x2340,
`BANNER` 1920x1080, `TITLE` flexible height) that platform payloads rarely match directly.
Prime Video only exposes unresized `pv-target-images` rasters (16:9 covershots, 4:3
packshots), so `PrimeVideoPlatform.cdnImage` rewrites the URL with Amazon CDN operations
chained as `._<op>_` before the extension: `_UR<w>,<h>_` resizes to the exact box and
`_CR<x>,<y>,<w>,<h>_` crops a region. Normalizing a 4:3 source to a 2:3 thumbnail goes
through an intermediate 4:3 canvas (`_UR3120,2340_CR780,0,1560,2340_`) so the crop
coordinates stay valid for any source size and the art is cropped, not stretched. Apply
the rewrite where the raw DTO is mapped to the domain model, keep non-CDN or
already-transformed URLs untouched, and cover each role (banner, thumbnail, episode
image, logo) with unit tests.

When a platform exposes dedicated portrait assets that cannot be harvested (Prime
Video's `poster2x3` images only ship through authenticated browse APIs and variant
carousels), carry the curated asset URLs in a default map indexed by show identifier
(Prime Video does this with `DEFAULT_THUMBNAILS`, migrated from the production
`attachment` table) and normalize them to `ImageType.THUMBNAIL` dimensions with a
pure resize operation (`_UR1560,2340_`). Titles absent from the map fall back to the
cropped raster so every show still resolves to canonical dimensions.

Structure the harvest in three stages, like the existing connectors:

1. Fetch raw payloads per country/language (`fetchPage`, `fetchWidgets`...).
2. Convert them into intermediate raw data (`PvShowData`, `PvSeasonData`, `PvEpisodeCard`...).
3. Map the raw data into `PlatformAnime`/`PlatformEpisode` per country.

## 6. Filtering and change detection

- **Release filters**: drop teasers/promotional cards (`TeaserEpisodeException`), upcoming or
  unlaunched episodes, entities that are not episodes, and releases that carry neither an audio
  track nor a subtitle usable by the target country.
- **Locales**: map platform track names (which are localized and decorated, e.g.
  `Français [Audiodescription]`, `日本語`) to ISO 639-1 codes, then to `Locale` values. The
  country default locale wins for the country's own language (`fr` on `fr-BE` stays `fr-BE`);
  keep every resolvable audio locale so `toReleaseModels()` emits one release per track. Throw
  `MissingAudioLocaleException` when a kept release has no resolvable audio.
- **Dates**: platform dates are localized strings — keep a per-language `DateTimeFormatter`
  list plus a fallback list, parse at midnight UTC, and drop unparseable dates.
- **Change flag**: never report `hasChanged` on raw payload differences alone. Detail pages
  embed per-request tokens, so the HTTP layer almost always reports a change. Keep a per-country
  cache of the previous `List<PlatformEpisode>` and compare with
  `List<PlatformEpisode>.hasSameContentAs` — report a change only when the normalized episodes
  actually differ.
- **Partial failures**: wrap each show/page fetch so one broken entry logs a warning and is
  skipped instead of failing the whole platform. Re-throw `CancellationException`.

## 7. Tests

- **Unit tests** (`jobs/src/test/kotlin/platforms/impl/`): back `SmartHttpClient` with a Ktor
  `MockEngine` routing the platform URLs to JSON fixtures, and cover mapping, filters, locales,
  pagination, movies/films, and the `hasChanged == false` case on identical payloads. When a
  fetch goes through a dedicated client, expose an `internal` override (see
  `PrimeVideoPlatform.widgetClient`) so tests can substitute a mock-backed `SmartHttpClient`
  instead of hitting the network.
- **Live tests** (`jobs/src/integrationTest/kotlin/`): gate them behind an environment variable
  (e.g. `RUN_PRIME_VIDEO_LIVE_TESTS=true`), like `CrunchyrollPlatformLiveTest`, so CI never
  depends on the external service. Exercise the real bootstrap (`GlobalContext`/`bootstrap()`)
  and `HarvestIngestionService` in at least one test.

## 8. Checklist

1. Add the `Platform` enum value if missing.
2. Create `platforms/impl/<Name>Platform.kt` implementing `StreamingPlatform` with
   `@Single(binds = [StreamingPlatform::class])`.
3. Normalize every field into `PlatformAnime`/`PlatformEpisode` (+ localizations, images,
   audio locales, release filters).
4. Implement per-country caching and `hasSameContentAs`-based change detection.
5. Add unit tests with `MockEngine` fixtures and an opt-in live test.
6. Run `./gradlew :jobs:test` and `./gradlew :jobs:integrationTest` (with the live-test env var
   for a real run), then the project-wide checks (`./gradlew test`, Qodana).
