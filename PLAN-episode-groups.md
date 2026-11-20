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

#### Points contractuels issus de la revue `:models` (à respecter à l'implémentation)

- **Visibilité** : `Country.visibleAudioLocales` est résolue par épisode sur
  **toutes** ses releases. Le repository doit donc fetcher toutes les locales
  des épisodes candidats (pas seulement celles passant les filtres SQL
  `audioLocales`/`platform`/`uncensored`/`from`/`to`), sinon le fallback EN/CJK
  sera calculé sur un ensemble tronqué. → fetch en deux temps : épisodes
  candidats filtrés, puis toutes leurs releases.
- **DTO** : `EpisodeObservation`/`EpisodeGroup` ne portent pas les champs du
  DTO final (`platform`, `url`, `uncensored`, `title`/`description`/`duration`,
  `lastUpdateDateTime`, `internalUrl`). Prévoir un second batch fetch par
  `releaseId`/`episodeId` dans le service (pattern `compose()` existant), ou
  enrichir le modèle — à trancher avant `:database`.
- **`searchTypes`** : filtré avant le bucketing ±2h (spec figée par les tests
  `:models`). Divergence possible vs un filtrage post-groupement — à valider
  contre le legacy lors de l'implémentation du service.

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

### Problèmes identifiés dans l'ancienne solution

| # | Sévérité | Problème | Résolution dans la nouvelle archi |
|---|---|---|---|
| 1 | Critique | `GroupedIndexer` = `object` singleton, collections mutables plain, séquences lazy sur structures vivantes → `ConcurrentModificationException` pendant que `:jobs` écrit | Pas d'index en mémoire : grouper pur sans état dans `:models`, données lues depuis Exposed |
| 2 | Critique | Index jamais invalidé (pas de `remove`/`update`) → divergence index/DB permanente ; `preIndex()` non atomique (`clear()` puis refill) → pages vides en cours de route | Vérité en base : `EpisodeRelease` upsertée par `save`, aucune donnée dérivée à maintenir |
| 3 | Haute | Fenêtre ±2h non transitive et dépendante de l'ordre d'insertion (`firstEntry()` = ancre hasardeuse) ; `group()` et l'index pouvaient diverger | On **conserve** la fenêtre ±2h (choix fonctionnel) mais déterministe : entrées triées par `releaseDateTime` avant bucketing, ancre = première observation, clé `(season, episodeType, number)` au lieu de `season*1000+number` |
| 4 | Haute | `CompositeKey` sans identité d'épisode (slug copié en String → renommage = orphelins) | Clé `(animeId, episodeType)` + identifiants UUID ; plus de slug dans la clé de groupement |
| 5 | Haute | Bug double enregistrement dans `countryIndex` (re-save idempotente du même variant) | Sans objet sans index |
| 6 | Haute | Pagination + tri 100% en mémoire sur tout le dataset (O(N log N) par page, tests JFR à 1M entrées) | Filtres + fenêtre par défaut poussés en SQL (dataset candidat borné) ; tri/pagination des groupes en mémoire sur cet ensemble borné — à mesurer ; `paginate()` SQL reste possible pour les endpoints sans regroupement |
| 7 | Haute | N+1 JPA (LAZY `mapping.anime`), entités JPA retenues dans le cache | Pattern `compose()` + batch `findAllByEpisodeIds` déjà en place |
| 8 | Moyenne | `preIndex()` = scan JPA complet au boot, fail-open si absent, pic mémoire | Zéro startup cost ; multi-replica cohérent car état en DB |
| 9 | Moyenne | `ZonedDateTime` ancien vs `LocalDateTime` nouveau | **Convention à documenter** : `LocalDateTime` = UTC à l'ingestion dans `:jobs` |
| 10 | Moyenne | `audioLocale: String` libre + `LangType` dépendant du pays | `audioLocale: Locale` typé ; règle de couverture par marché reproduite via `Country.subtitleLocales`/`voiceLocale` — matrice pays×locale à tester |
| 11 | Moyenne | Cache key avec `Array` dans data class → misses permanents + invalidation tout-ou-rien | Pas de cache dans un premier temps ; si besoin plus tard : clé stable (`Set`/`List`) + TTL ou invalidation sur `EpisodeReleaseRepository.save` |
| 12 | Moyenne | `sort` chaîne libre → champ inconnu ignoré silencieusement → pagination instable ; DTO avec `season`/`number` en String | Whitelist de champs triables → colonnes SQL ; DTO avec types structurés |
| 13 | Haute (design) | Index heap = replicas divergentes, scaling horizontal impossible | État en DB uniquement |

### Arbitrages issus de l'analyse

- **La fenêtre ±2h est conservée** (choix fonctionnel) mais rendue déterministe :
  tri par `releaseDateTime` avant bucketing. L'alternative "date canonique
  `Episode.releaseDateTime`" reste possible si la fenêtre pose problème en
  pratique — à réévaluer après les tests.
- **`LocalDateTime` = UTC** : vérifier que `:jobs` écrit bien en UTC
  (`TimeUtils.now()` existe dans le framework) ; documenter dans `:models`.

### Mesures sur données de production (copie `shikkanime_bench`)

Ancien indexer exécuté tel quel (`preIndex` + `GroupedIndexer` +
`NewGroupIndexer` + `GroupedEpisodeService.findAllBy`) — 58 200 variants,
33 381 mappings, 1 377 animes :

| Mesure | Résultat |
|---|---|
| `preIndex()` | 5,7 s, heap 42 → 90 MB (+48 MB) |
| Groupes `GroupedIndexer` / `NewGroupIndexer` | 35 403 / 35 847 (split couverture ≈ +1,3 %) |
| Pagination en mémoire | 4–20 ms/page, tri complet 21 ms |
| Chemin complet `findAllBy` | ~15–90 ms/page |

Constats : à ce volume la perf n'est **pas** le problème (les tests JFR à
1M entrées étaient synthétiques) — les vrais problèmes restent la correction
(staleness, concurrence). `preIndex()` ne clear jamais les indexeurs et est
appelé à chaud par des endpoints admin → divergence garantie. Données :
3 336 variants datés < 2000 (erreurs de scrape), un groupe fourre-tout de
582 entries.

### Suite de tests de non-régression

`models/src/test/kotlin/EpisodeGrouperTest.kt` et `LangTypeTest.kt` sont la
spec exécutable de tout l'indexer (fenêtre ±2h ancrée, split couverture
LangType, fallback EN, exclusion hors-locales pays, dédup, tri). Écrits
test-first : ils ne compilent qu'une fois `EpisodeGrouper`/`LangType`/
`visibleAudioLocales` implémentés dans `:models` — ils font office de filet
de non-régression pour le portage.

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
