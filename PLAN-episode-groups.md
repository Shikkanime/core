# Plan — Groupes d'épisodes (API publique)

Portage de la feature `new-group-indexer` (ancienne archi) vers la multi-module.
Endpoint `GET /v1/episode-groups` : derniers groupes d'épisodes avec filtres et tris.

## Sémantique de groupement (validée)

- Bucket par `(animeId, episodeType)` ; une observation (release) rejoint le bucket
  si `releaseDateTime` ∈ `[bucket.anchor - 2h, bucket.anchor + 2h]` (ancre = 1re observation).
- Split du bucket par **signature de couverture exacte** : `Map<Locale, Set<(season, number)>>`.
  Fidèle à `NewGroupIndexer` ; une release VF en plus crée un groupe séparé.
- Tri des groupes : `releaseDateTime`, tie-breaks `animeSlug`, `episodeType`.

## Améliorations par rapport à l'ancien indexeur

- Grouper **pur et sans état** dans `:models` (`EpisodeGrouper`) — fini le singleton
  mutable, le `buckets.find` O(n²), le hack `season*1000+number`.
- Regroupement en post-traitement Kotlin sur des lignes SQL filtrées ; pas de table
  `episode_group` persistée, pas de cache dans un premier temps.

## Modules

### `:models`
- `EpisodeGroup` : animeId, animeSlug, episodeType, releaseDateTime, signature,
  membres (episodes + leurs `EpisodeRelease`).
- `EpisodeGrouper.group(rows): List<EpisodeGroup>` — fonction pure, tri + pagination
  non inclus (fait par le service).

### `:database`
- `EpisodeGroupRepository` (Exposed) : jointure `Episode ⋈ Anime ⋈ EpisodeRelease`,
  `select(EpisodeTable.columns)` + `wrapRows`, `where` sur les filtres.
- `EpisodeGroupService` : fetch → `EpisodeGrouper` → tri → `Page`/`PageRequest`.
- Filtres : `animeId`, `episodeType`, `platform`, `audioLocales` (multi), `country`
  (expandé vers ses locales via `Country.defaultLocale`), `from`/`to` sur
  **`release.releaseDateTime`**, `uncensored`.
- Défaut sans `from`/`to` : fenêtre bornée (constante, ex. −30j / +30j).

### `:api`
- `GET /v1/episode-groups` : `@Valid @ModelAttribute PageQuery` (page/size) +
  `@QueryParam` filtres ; `sort` whitelisté multi-champs (`releaseDateTime`,
  `animeSlug`, `episodeType`, `season`, `number`, `localeCount`, `field:asc|desc`).
- `EpisodeGroupDto` : `anime` (AnimeDto), `releaseDateTime`, `episodeType`,
  plage seasons/numbers, `episodes` (id, season, number), `releases`
  (EpisodeReleaseDto existant), `title`/`description`/`duration` nullable.
- Un DTO par fichier, mapping `toDto()` en extension, OpenAPI documenté.

## Tests

- `:models` : tests unitaires du grouper (buckets ±2h, split couverture, tri).
- `:database` : test repository/service sur fixtures (filtres, pagination).
- `:api` : test controller/DTO si le module en a.

## Non-goals

Pas de persistance du groupe, pas de cache, pas d'exposition `:admin`/`:site`
(le service `:database` reste réutilisable plus tard).

## Ordre d'implémentation

1. `EpisodeGroup` + `EpisodeGrouper` dans `:models` + tests.
2. Repository + service `:database` + tests.
3. DTO + controller `:api` + tests.
4. Vérification build/tests, revue guidelines.

## Points ouverts

- Fenêtre par défaut exacte (proposition : −30j / +30j autour de now).
- `country` → locales : `defaultLocale` seul ou extension multi-locales ?
- Format du param `sort` (CSV `field:dir` vs paramètres séparés).
