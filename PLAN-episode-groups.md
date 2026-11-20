# Plan — Groupes d'épisodes (`/v1/episode-groups`)

Portage du fonctionnel de la branche `new-group-indexer` (ancienne archi) vers la
multi-module `:models` / `:database` / `:api`. "Variants" → "releases".

## 1. Fonctionnel repris de la branche initiale

### Groupement

- Bucket par `(country, anime, episodeType)`. Une release rejoint le bucket si son
  `releaseDateTime` tombe dans la fenêtre ±2h (comportement `NewGroupIndexer` :
  ancre = `releaseDateTime` du bucket, pas de chaînage transitif).
- Split du bucket par **signature de couverture exacte**
  `Map<LangType, Set<(season, number)>>` : deux releases ne partagent un groupe que
  si l'ensemble des épisodes couverts par chaque LangType est identique.
- Tri par défaut des groupes : `releaseDateTime`, tie-breaks `animeSlug`,
  `episodeType`.

### LangType et locales par pays

- `LangType { SUBTITLES, VOICE }` réintroduit dans `:models`.
- `Country` étendu : `subtitleLocales = {JA_JP, ZH_CN, KO_KR}`,
  `fallbackSubtitleLocale = EN_US`, `voiceLocale = defaultLocale`
  (fr-FR→FR, fr-BE→BE, de-DE→DE, pl-PL→PL…).
- **Locales visibles d'un épisode pour un pays** (reproduit
  `LocaleUtils.getAllowedLocales`, appliqué par épisode sur ses releases) :
  1. `subtitleLocales ∩ locales dispo` ; si vide → `fallbackSubtitleLocale ∩ dispo`
  2. `voiceLocale` ajoutée si disponible
  3. releases hors de cet ensemble : exclues de la vue du pays
- `LangType.fromAudioLocale(country, locale)` : `VOICE` si
  `locale == country.voiceLocale`, sinon `SUBTITLES`.
- Filtre `searchTypes=VOICE,SUBTITLES` (comme l'ancien endpoint).

### Filtres, tris, pagination (reprise + extension)

- Anciens : `country` (nullable), `searchTypes`, `sort` multi-champs
  (`releaseDateTime`, `episodeType`, `season`, `number`, `animeName`),
  `page`/`limit`.
- Nouveaux (standard complet) : `animeId`, `episodeType`, `platform`,
  `audioLocales` (multi), `from`/`to` sur `release.releaseDateTime`, `uncensored`.
- Pagination : `Page`/`PageRequest`/`PageDto` du framework (1-based, size ≤ 100).

### Contenu du groupe (reproduit `GroupedEpisode`/`GroupedEpisodeDto`)

- `anime` ( AnimeDto ), `releaseDateTime` (min des releases), `lastUpdateDateTime`
  (max `updatedAt` des épisodes), `season`/`number` en plages
  (`min - max` si différent), `episodeType`, `title`/`description`/`duration`
  **uniquement si le groupe contient un seul épisode**, `internalUrl`,
  ids des épisodes membres, et `sources` = releases (platform, url,
  audioLocale, uncensored, releaseDateTime).

### Consommateurs futurs (non-goals immédiats)

Site home, SEO, vue weekly : le service `:database` doit rester réutilisable ;
le cache (`GroupedEpisodeCacheService` + invalidation par entité) est reporté.

## 2. Architecture cible

### `:models` (pur, zéro dépendance)

- `LangType`, extension de `Country` (subtitle/fallback/voice locales).
- `EpisodeGroup` (+ entrée membre) : animeId, animeSlug, episodeType,
  releaseDateTime, signature LangType, épisodes + releases membres.
- `EpisodeGrouper.group(rows, country): List<EpisodeGroup>` : fonction pure,
  sans état — corrige le singleton mutable, le `find` O(n²) et le hack
  `season*1000+number` de `NewGroupIndexer`.

### `:database`

- `EpisodeGroupRepository` : jointure `Episode ⋈ Anime ⋈ EpisodeRelease`
  (`select(EpisodeTable.columns)` + `wrapRows`), filtres en `where`,
  fenêtre par défaut bornée quand `from`/`to` absents (proposition : −30j/+30j).
- `EpisodeGroupService` : fetch → `EpisodeGrouper` → tri → `Page`/`PageRequest`.

### `:api`

- `GET /v1/episode-groups` : `@Valid @ModelAttribute PageQuery` + `@QueryParam`
  filtres ; `sort` whitelisté au format CSV `field:asc|desc`.
- `EpisodeGroupDto` (+ DTOs membres si nécessaire), `toDto()` en extension,
  un DTO par fichier, OpenAPI documenté.

## 3. Analyse critique de l'ancienne solution (sous-agent)

Mission confiée à un sous-agent d'exploration : inventorier les problèmes de
l'implémentation actuelle (`GroupedIndexer`/`NewGroupIndexer`, cache, requêtes
JPA, sémantique ±2h, pagination en mémoire…) et proposer des correctifs dans la
nouvelle architecture. Les résultats sont reportés ci-dessous et arbitrés avant
implémentation.

<!-- À REMPLIR avec les findings du sous-agent -->

## 4. Tests

- `:models` : grouper — buckets ±2h, split couverture LangType, fallback EN,
  exclusion hors-locales pays, tri.
- `:database` : repository/service — filtres, pagination, fenêtre par défaut.
- `:api` : controller/DTO.

## 5. Non-goals

Pas de table `episode_group` persistée, pas de cache, pas de vue weekly,
pas d'exposition `:admin`/`:site`.

## 6. Ordre d'implémentation

1. `LangType` + `Country` + `EpisodeGroup`/`EpisodeGrouper` (`:models`) + tests.
2. Repository + service (`:database`) + tests.
3. DTO + controller (`:api`) + tests.
4. Build/tests, revue guidelines.

## 7. Points ouverts

- `country` : optionnel avec défaut FR, ou obligatoire ? (ancien : nullable)
- Fenêtre par défaut exacte (proposition : −30j / +30j autour de now).
- `internalUrl` : réintroduit (base URL + slug) ou deep-link par ids d'épisodes ?
- Sort `animeName` : slug ou titre localisé (join localization) ?
