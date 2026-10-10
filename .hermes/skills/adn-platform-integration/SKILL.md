---
name: adn-platform-integration
description: Integrate and maintain the ADN streaming platform in core.
version: 1.0.0
author: Ziedelth
license: MIT
platforms: [linux, macos]
metadata:
  hermes:
    tags: [adn, streaming-platform, ingestion, shikkanime]
    related_skills: [shikkanime-core-backend]
---

# ADN Platform Integration Skill

## Overview

This skill describes the architecture, field mapping, and maintenance procedures for the
**Animation Digital Network (ADN)** ingestion provider in the Shikkanime backend.

ADN is integrated as a `StreamingPlatform` under
`jobs/src/main/kotlin/platforms/impl/AnimationDigitalNetworkPlatform.kt`.

## When to Use

- Extending or maintaining the ADN ingestion provider in `jobs`.
- Adding new metadata fields mapped from the ADN video or show payloads.
- Debugging episode categorization (Film, Special, Episode) or audio locale fan-out.
- Writing unit or performance tests for ADN calendar ingestion.

## Architecture & Endpoints

### API Contracts
- **Base URL**: `https://gw.api.animationdigitalnetwork.com/`
- **Calendar Endpoint**: `video/calendar?date=YYYY-MM-DD`
- **Required Headers**:
  - `Accept-Language: fr`
  - `X-Source: Web`
  - `X-Target-Distribution: fr`

### Response Structure
- The calendar response returns scheduled and published videos from the requested day onward.
- Each video embeds an inline `show` object with series metadata.

## Field Mapping & Business Rules

### Episode Mapping (`PlatformEpisode`)
- `id`: Raw ADN video ID string.
- `title`: Video name (chapter or episode title).
- `description`: Textual summary from `summary`.
- `image`: Direct high-definition preview URL from `video.image2x`.
- `releaseDateTime`: UTC zoned date-time from `releaseDate`.
- `season`: Extracted from `video.season?.toIntOrNull() ?: 1`.
- `number`: Parsed sequence number (-1 if non-numbered).
- `episodeType`: Resolved via `getNumberAndEpisodeType`:
  - `FILM`: `shortNumber` matches `Film(?: (\d*))?` or `videoType == MOV` or `showType == AdnType.MOV`.
  - `SPECIAL`: `shortNumber` matches `(?:Épisode spécial|OAV)` or `videoType == OAV`
    or `showType == AdnType.OAV` or has dot.
  - `EPISODE`: Standard television broadcast episode.
- `duration`: Run-time in seconds from `duration`.
- `url`: Direct platform playback URL from `url`.
- `audioLocale`: Resolved through `LOCALE_MAP` (`vostf` -> `ja-JP`, `vf` -> `fr-FR`).
- `uncensored`: True if video title contains `(NC)` or `Non censuré` (case-insensitive).
- `original`: True if the audio track is the primary language (`index == 0`).

### Multi-Language Fan-Out
- ADN videos carrying multiple languages (e.g. VOSTFR and VF) are fanned out using `flatMap`
  into separate `PlatformEpisode` items with their respective `audioLocale`.

### Anime Mapping (`PlatformAnime`)
- `id`: Raw ADN show ID string.
- `title`: Canonical anime title, cleaned via `cleanAnimeName` (stripping season suffixes,
  parts, and roman numerals). Prioritizes `show.shortTitle` when present.
- `thumbnail`: Portrait poster URL (1080x1543) with logo.
- `banner`: Landscape header banner (1920x1080) derived from `imageHorizontal2x` (null if absent).

### Exclusion Filters
- Content with types `PV` or `BONUS` is dropped.
- Short numbers starting with trailer indicators (`Bande-annonce`, `Opening`, `Making-of`,
  `Court-métrage`) are dropped.
- Shows without `"Animation "` in their genre tags are excluded.

## Verification & Testing

### Test Suite Execution
Run tests without hitting any live network via:
```bash
./gradlew :jobs:test :jobs:perfTest
```

### Writing Tests
- Use `HttpClient(MockEngine)` with `SmartHttpClient`.
- Structure tests with JUnit 5 `@Nested`, `@DisplayName`, and Given/When/Then comments.
- Test both nominal and edge cases (e.g. unnumbered films, unparseable decimals, fan-out).

## Pitfalls

1. **Live Network Forbidden**: Never emit actual HTTP requests during tests.
2. **Regex Precompilation**: All regex patterns (`DIMENSION_REGEX`, `MOVIE_REGEX`, etc.)
   must be precompiled to respect execution budgets.
3. **Explicit Data Class Properties**: Do not add default fallback values (like `null`)
   to `PlatformEpisode` or `PlatformAnime`; enforce all mandatory metadata at extraction.
