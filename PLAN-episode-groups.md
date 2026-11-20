# Plan — Groupes d'épisodes (API publique)

Portage **fidèle** de la feature `new-group-indexer` (ancienne archi) vers la
multi-module. Endpoint `GET /v1/episode-groups` : derniers groupes d'épisodes
avec filtres et tris. "Variants" de l'ancien code = "releases" désormais.

## Sémantique de groupement (identique à l'ancienne branche)

- Bucket par `(country, animeId, episodeType)` ; une release rejoint le bucket si
  `release.releaseDateTime` ∈ `[bucket.anchor - 2h, bucket.anchor + 2h]`
  (ancre = première observation, comme `NewGroupIndexer`).
- **Split par signature de couverture exacte** `Map<LangType, Set<(season, number)>>`
  — identique à l'ancien `NewGroupIndexer.buildIndex`.
- Tri des groupes : `releaseDateTime`, tie-breaks `animeSlug`, `episodeType`.

## LangType réintroduit (adapté aux pays)

Nouveau `LangType { SUBTITLES, VOICE }` dans `:models`, avec :

- `Country` étendu : `subtitleLocales = {JA_JP, ZH_CN, KO_KR}`,
  `fallbackSubtitleLocale = EN_US`, `voiceLocale = defaultLocale`
  (fr-FR pour FR, fr-BE pour BE, de-DE pour DE, etc.).
- **Locales visibles pour un pays** (par épisode, sur l'ensemble de ses releases) :
  1. `subtitleLocales ∩ disponibles` — si vide → `fallbackSubtitleLocale ∩ dispo`
  2. `voiceLocale` ajoutée si disponible
  3. Les releases hors de cet ensemble sont **exclues** de la vue de ce pays
     (reproduit `LocaleUtils.getAllowedLocales` + le filtrage au harvest).
- `LangType.fromAudioLocale(country, locale)` : `VOICE` si `locale == country.voiceLocale`,
  sinon `SUBTITLES`.

## Améliorations vs ancien indexeur (non-fonctionnelles)

- Grouper **pur et sans état** (`:models`) : fini le singleton mutable,
  le `buckets.find` O(n²), le hack `season*1000+number` (clé data class à la place).
- Regroupement en post-traitement Kotlin sur lignes SQL filtrées ;
  pas de table `episode_group`, pas de cache dans un premier temps.

## Modules

### `:models`
- `LangType` + `Country.subtitleLocales`/`fallbackSubtitleLocale`/`voiceLocale`.
- `EpisodeGroup` : animeId, animeSlug, episodeType, releaseDateTime, signature,
  membres (episodes + releases).
- `EpisodeGrouper.group(rows, country): List<EpisodeGroup>` — fonction pure.

### `:database`
- `EpisodeGroupRepository` : `Episode ⋈ Anime ⋈ EpisodeRelease`,
  `select(EpisodeTable.columns)` + `wrapRows`, `where` sur filtres.
- `EpisodeGroupService` : fetch → `EpisodeGrouper` → tri → `Page`/`PageRequest`.
- Filtres : `country` (défaut FR), `searchTypes` (VOICE/SUBTITLES), `animeId`,
  `episodeType`, `platform`, `audioLocales` (multi), `from`/`to` sur
  `release.releaseDateTime`, `uncensored`.
- Défaut sans `from`/`to` : fenêtre bornée (constante, ex. −30j / +30j).

### `:api`
- `GET /v1/episode-groups` : `PageQuery` + `@QueryParam` filtres ;
  `sort` whitelisté (`releaseDateTime`, `animeSlug`, `episodeType`, `season`,
  `number`, `localeCount` ; format `field:asc|desc` CSV).
- `EpisodeGroupDto` : `anime` (AnimeDto), `country`, `releaseDateTime`,
  `episodeType`, plage seasons/numbers, `episodes` (id, season, number),
  `releases` (EpisodeReleaseDto), `searchTypes` écho ou couverture LangType,
  `title`/`description`/`duration` nullable.
- Un DTO par fichier, `toDto()` en extension, OpenAPI documenté.

## Tests

- `:models` : grouper (buckets ±2h, split couverture LangType, fallback EN,
  exclusion hors-locales pays, tri).
- `:database` : repository/service (filtres, pagination, fenêtre par défaut).
- `:api` : controller/DTO.

## Non-goals (pour l'instant)

Pas de persistance du groupe, pas de cache (`GroupedEpisodeCacheService`
viendra si perfs), pas de vue weekly, pas d'exposition `:admin`/`:site`.

## Ordre d'implémentation

1. `LangType`, extension `Country`, `EpisodeGroup` + `EpisodeGrouper` (`:models`) + tests.
2. Repository + service (`:database`) + tests.
3. DTO + controller (`:api`) + tests.
4. Build/tests, revue guidelines.

## Points ouverts

- `country` : param optionnel avec défaut FR, ou obligatoire ? (ancien : nullable)
- Fenêtre par défaut exacte (proposition : −30j / +30j autour de now).
- `searchTypes` : nom du param conservé (`VOICE,SUBTITLES`) comme avant ?
