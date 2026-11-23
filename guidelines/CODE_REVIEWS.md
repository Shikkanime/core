# Code Reviews Log

History of the code-review findings reported on this project and the fixes applied. Each entry
records what was flagged, why it was wrong, and the pattern that resolved it — with a short excerpt
(5-6 lines at most) that points to the solution, not a full implementation.

Entries are appended newest last. When a finding reveals a reusable convention, the rule itself
belongs in the appropriate guide (`CODE_STYLE.md`, `KOTLIN_CONVENTIONS.md`, …), not only here.

## Template

```markdown
### YYYY-MM-DD — <short title>

- **Where**: `path/to/file.kt` (or the Flutter/webapp area)
- **Finding**: what the review pointed out.
- **Fix**: the approach that resolved it.

```kotlin
// 5-6 lines max excerpt of the pattern
\```
```

### 2026-11-12 — Lambdas replaced by callable references in jobs

- **Where**: `jobs/src/main/kotlin/impl/FetchLatestEpisodesJob.kt`, `HarvestIngestionService.kt`,
  `SmartHttpClient.kt`, `platforms/impl/*Platform.kt`, plus test files.
- **Finding**: `.map { it.fetchLatestEpisodes() }`, `.count { it.hasChanged }` and similar lambdas
  should use callable references (`StreamingPlatform::fetchLatestEpisodes`,
  `SmartResponse<*>::hasChanged`) everywhere a direct member access is possible.
- **Fix**: applied repo-wide; the rule already existed in `KOTLIN_CONVENTIONS.md`. Generic receivers
  need a star projection (`SmartResponse<*>::hasChanged`); a `suspend` function reference cannot be
  passed to the non-suspend `map` transform, so `map { it.fetchLatestEpisodes() }` stays a lambda.

```kotlin
val changed = harvest.count(SmartResponse<*>::hasChanged)
val allAudioLocales = group.flatMap(PlatformEpisode::audioLocales).distinct()
```

### 2026-11-12 — `archiveResponseBody` split into focused helpers

- **Where**: `jobs/src/main/kotlin/SmartHttpClient.kt`.
- **Finding**: `archiveResponseBody` mixed directory resolution, latest/previous rotation, history
  append and pruning in one hard-to-read function.
- **Fix**: extracted `dumpDirFor`, `archiveLatestVersion` and `appendToHistory`; the public function
  is now a short orchestration. Rule added to `CODE_STYLE.md` (Methods and Functions).

```kotlin
val keyDir = dumpDirFor(cacheKey) ?: return@runCatching
archiveLatestVersion(keyDir, cacheKey, url, bodyText)
val timestamp = appendToHistory(keyDir, bodyText)
pruneStaleDumpDirs(timestamp)
```

### 2026-11-13 — Crunchyroll platform readability pass

- **Where**: `jobs/src/main/kotlin/platforms/impl/CrunchyrollPlatform.kt`.
- **Finding**: `getNumberAndEpisodeType` mixed every detection rule in one `when` plus three
  overrides; `!trimmedNumberString.isNullOrBlank()` read poorly; `CrunchyrollImages` repeated the
  same widest-source logic three times; `episodeImage` added a series-artwork fallback the legacy
  implementation never had; the token mint duplicated its expiry computation; several lambdas could
  be callable references or shared predicates.
- **Fix**: split the type detection into `detectEpisodeType`, `isMovieRelease`, `hasMovieSlug`,
  `isUnnumberedLongRelease`, `isSpecialSeason`, `specialEpisodeNumber`, `hasNonNumericEpisodeLabel`
  and `isOadRelease`; added the `String?.isNotNullOrBlank()` inverse extension; deduplicated the
  image getters through `widestSource`; dropped `episodeImage` so the episode image is again the
  browse thumbnail with the title logo as the never-blank fallback; extracted `fetchAccessToken`
  with `expiresAtMillis`/`tokenLifetimeMillis` response extensions; shared the country-language
  predicate as a local lambda and the slug fallback through `firstNonBlank`.

```kotlin
val matchesCountryLanguage = { locale: String? ->
    locale?.substringBefore('-')?.equals(countryLanguage, ignoreCase = true) == true
}
val isDubbed = matchesCountryLanguage(metadata.audioLocale)
val hasSubtitle = metadata.subtitleLocales.orEmpty().any(matchesCountryLanguage)
```

### 2026-11-13 — Crunchyroll title fallback and single token mint

- **Where**: `jobs/src/main/kotlin/platforms/impl/CrunchyrollPlatform.kt`.
- **Finding**: `toPlatformAnime` fell back to the raw series id when no title resolved, which
  persisted a meaningless anime name; `fetchAccessToken` could query the token endpoint twice
  although every mint returns a fresh token; Qodana flagged an unclear `?:`/`+` precedence.
- **Fix**: `toPlatformAnime` now returns `null` when the series carries no usable title and the
  episode is skipped; `fetchAccessToken` drops the cached token payload up front and mints once;
  the expiry fallback is parenthesized. A `qodana.recommended` scan of `:jobs` reports 0 problems.

```kotlin
val animeTitle = cleanAnimeTitle(
    firstNonBlank(rawTitle, seriesMetadata?.title, title)
).first.sanitizeText()
    ?: return null
```

### 2026-11-14 — Episode image must not use the series title logo of the episode id

- **Where**: `jobs/src/main/kotlin/platforms/impl/CrunchyrollPlatform.kt`.
- **Finding**: `CrunchyrollBrowseObject.fullHDTitle` builds `keyart/<id>-title_logo`, a URL that
- **only exists for series**; using it as the episode image fallback produced a broken artwork URL
  keyed on the episode identifier.
- **Fix**: the episode image is again the browse thumbnail, falling back to the parent series'
  `fullHDTitle` — the anime artwork, which is what the property is meant for.

```kotlin
image = firstNonBlank(
    images?.fullHDThumbnail,
    series.fullHDTitle.takeIf { series.id.isNotBlank() }
).orEmpty(),
```

### 2026-11-14 — `toPlatformEpisode` realigned on the legacy mapping

- **Where**: `jobs/src/main/kotlin/platforms/impl/CrunchyrollPlatform.kt`.
- **Finding**: the episode mapping had drifted from the legacy implementation: the image fell
  back to series artwork, the watch URL tried metadata and series slugs, the season filtered
  non-positive numbers, and the localization title fell back to the metadata title.
- **Fix**: restored the legacy semantics — image is the browse thumbnail or empty, the URL is
  `watch/<id>/<browse slug or empty>`, the season is `forcedSeason ?: seasonNumber ?: 1`, and the
  localization reads the browse object's own title and description. Only the changes required by
  the new model were kept (sanitized texts, nullable-safe reads, resolved audio locale).

```kotlin
image = images?.fullHDThumbnail.orEmpty(),
url = "$BASE_URL/${country.code.lowercase()}/watch/$id/${slugTitle.orEmpty()}",
```

### 2026-11-15 — Guideline alignment pass across models, database, admin, api and jobs

- **Where**: every module — `models`, `database`, `api`, `admin`, `jobs`.
- **Finding**: a multi-agent audit flagged `ResponseEntity<Any>` where `ResponseEntity<*>` fits,
  repeated `episode.episode.animeId`-style nested access, a leftover
  `Clock.System.now().toLocalDateTime(TimeZone.UTC)`, a `String?.isNotNullOrBlank()` private to one
  platform, `Platform*` harvest types and the ingestion service in the wrong packages, public Exposed
  entities/tables leaking out of `:database`, DTO conversions taking external parameters, DTOs in one
  flat package, missing KDoc, single-line `if/else`, a logged default password and a fragile
  `findAllByRoles` query.
- **Fix**: `EpisodeWithChildren`/`AnimeWithChildren`/`PlatformEpisode` gained projection delegates;
  `isNotNullOrBlank` moved to `:models`; `Platform*` types moved to `platforms.models` and
  `HarvestIngestionService` to `jobs.services`; entities/tables are `internal` and `User`/`Simulcast`
  domain models replaced the leaked entities in every public port; admin conversions became
  parameterless through `AnimeWithMetadataLinks`/`EpisodeWithMetadataLinks`/`EpisodeWithAnimeTitle`
  sources; api DTOs regrouped under `dtos/anime|episode|image`; `TimeUtils.now()` replaced the manual
  UTC conversion; KDoc/`if-else`/method-reference/style issues fixed; the default password is no
  longer logged; `findAllByRoles` filters on the model. New rules recorded in `MODELS.md`
  (projection delegates), `TESTING.md` (stub derived members on mocks), `KOTLIN_CONVENTIONS.md`
  (`TimeUtils.now()`).

```kotlin
val animeId: Uuid?
    get() = episode.animeId

return episodes.toDto { episode ->
    EpisodeWithAnimeTitle(episode, animes[episode.animeId]?.normalizedTitle.orEmpty())
        .toAdminLatestDto()
}
```

### 2026-11-19 — Qodana local pass: imports optimisés et 6 findings corrigés

- **Where**: repo-wide — IntelliJ `optimize_imports` via the IDE MCP on every touched Kotlin
  file, then `qodana scan` (`qodana-jvm-community`, `qodana.recommended`).
- **Finding**: six problems — three `UnusedSymbol` on Koin-bound use case impls, two
  `KDocUnresolvedReference` (`SimulcastService`, `HarvestIngestionService`), one
  `ArrayInDataClass` on `User.identifier: ByteArray?`.
- **Fix**: imports optimized by the IDE; the two KDoc links resolved by importing the service
  types; `User.identifier` became `List<Byte>?` (mapped with `toList()` at the entity boundary)
  so data-class equality stays structural; `UnusedSymbol` excluded for
  `admin/src/main/kotlin/usecases/impl` in `qodana.yaml`, same rationale as the existing
  `jobs/impl` exclusion (Koin instantiates the impls reflectively). Second scan: 0 problems.
  The `@Suppress("unused")` on Koin/reflective classes stays: removing it would reintroduce
  the very warnings it silences.

```kotlin
val identifier: List<Byte>?, // was ByteArray? — ArrayInDataClass

// qodana.yaml
- name: UnusedSymbol
  paths:
    - admin/src/main/kotlin/usecases/impl
```

### 2026-11-23 — PrimeVideoPlatform: JsonObject banni, client Ktor dédié, ContentType

- **Where**: `jobs/src/main/kotlin/platforms/impl/PrimeVideoPlatform.kt` (review of the
  Prime Video connector on `feat/prime-video-platform`).
- **Finding**: three points — (1) `java.net.http.HttpRequest` was used for the widget
  pagination instead of the Ktor client from the framework; (2) the payload was navigated
  through `JsonObject`/`JsonElement`, which the project bans in favor of `@Serializable` DTOs;
  (3) the `Accept` header carried the raw string `"application/json"` instead of a
  `ContentType`.
- **Fix**: the widget endpoint now goes through a dedicated `SmartHttpClient` wrapping a Ktor
  `HttpClient(OkHttp)` built like `createHttpClient()` but with `followRedirects = false` —
  the bot check's redirect loop is a Ktor `HttpRedirect` plugin artifact, and the redirect-free
  client negotiates the JSON payload (verified live: 200 vs `SendCountExceedException`). The
  whole harvest was rewritten on typed DTOs (`PrimeVideoPageResponse`, `PrimeVideoState`,
  `PrimeVideoEpisodeEntity`, …) matching the other connectors, and the header uses
  `ContentType.Application.Json.toString()`.

```kotlin
internal var widgetClient: SmartHttpClient = SmartHttpClient(
    client = HttpClient(OkHttp) {
        followRedirects = false
        install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 30_000 }
        install(ContentNegotiation) { json(defaultJson) }
    }
)

HttpHeaders.Accept to ContentType.Application.Json.toString()
```

### 2026-11-23 — PrimeVideoPlatform: images CDN normalisées aux dimensions canoniques

- **Where**: `jobs/src/main/kotlin/platforms/impl/PrimeVideoPlatform.kt` +
  `PrimeVideoPlatformTest.kt` + `docs/STREAMING_PLATFORMS.md` (follow-up review on
  `feat/prime-video-platform`).
- **Finding**: harvested Prime Video image URLs were raw `pv-target-images` assets whose
  dimensions vary by role (3840x2160 covershots, 1600x1200/2560x1920 4:3 packshots,
  960px episode rasters), so stored `BANNER`/`THUMBNAIL` images never matched the
  canonical `ImageType` dimensions used by the other platforms.
- **Fix**: `cdnImage()` rewrites Amazon CDN URLs with chained `._<op>_` operations before
  the file extension — `_UR<w>,<h>_` resizes to the exact box, `_CR<x>,<y>,<w>,<h>_`
  crops a region. 16:9 sources get `_UR1920,1080_`; 4:3 sources are normalized to a 4:3
  canvas first, then center-cropped to the target aspect (`_UR3120,2340_CR780,0,1560,2340_`
  for `THUMBNAIL`, `_UR1920,1440_CR0,180,1920,1080_` for `BANNER`), so art is cropped
  instead of stretched. Non-CDN or already-transformed URLs pass through unchanged, and
  the pattern is documented in `docs/STREAMING_PLATFORMS.md` for future platforms.

```kotlin
private const val CDN_BANNER = "UR1920,1080"
private const val CDN_BANNER_FROM_4X3 = "UR1920,1440_CR0,180,1920,1080"
private const val CDN_THUMBNAIL_FROM_4X3 = "UR3120,2340_CR780,0,1560,2340"
```

### 2026-11-23 — PrimeVideoPlatform: posters 2:3 dédiés non moissonnables, map curée

- **Where**: `jobs/src/main/kotlin/platforms/impl/PrimeVideoPlatform.kt` +
  `PrimeVideoPlatformTest.kt` + `docs/STREAMING_PLATFORMS.md` (follow-up review on
  `feat/prime-video-platform`).
- **Finding**: the center-cropped 4:3 packshot used for `THUMBNAIL` loses too much of the
  key art — the title is often cut off, unlike the dedicated Prime Video portrait posters
  (`poster2x3`, 2000x3000) displayed by the official apps and present in production.
- **Investigation**: the `poster2x3` raster is absent from every public surface reachable
  by the connector — detail page payloads only carry `covershot`/`heroshot` (16:9) and
  `packshot`/`titleshot` (4:3), `getDetailWidgets` rejects non-`EpisodeList` widget types,
  genre/search rails expose `cover` only, and `VariantCarousel` entities (which do carry
  `poster2x3`) are rare and never describe the harvested title itself. The production
  thumbnails were manually curated through the legacy `simulcast.image` configuration.
- **Fix**: the seven curated `poster2x3` URLs found in the production `attachment` table
  were migrated into `DEFAULT_THUMBNAILS` (indexed by compact show id, injectable through
  the `thumbnails` constructor parameter) and are normalized with a pure resize
  (`_UR1560,2340_`). Shows absent from the map keep the center-cropped packshot fallback
  so every title still resolves to canonical dimensions. `cdnImage` now matches any
  `amazon.com/images/` host (`m.media-amazon.com` and `images-eu.ssl-images-amazon.com`).

```kotlin
thumbnailImage = thumbnails[showId]
    ?.let { cdnImage(it, CDN_THUMBNAIL) }
    ?: listOfNotNull(showImages?.packshot, showImages?.titleshot)
        .firstOrNull(String::isNotBlank)
        ?.let { cdnImage(it, CDN_THUMBNAIL_FROM_4X3) }
```
