# Models Module Guidelines

The `:models` module represents the core domain objects shared across all modules.

## Principles

1. **Zero Framework Dependencies**: No ORM annotations, no Ktor imports, no Jackson/serialization annotations unless explicitly shared.
2. **Immutability**: Always use `data class` with `val` properties.
3. **Reference by ID**: Avoid deep object nesting in base domain models. Reference related entities by `UUID` (e.g., `animeId: UUID`).
4. **Projections for Composites**: When an application requires an aggregated view, define a projection class (e.g., `EpisodeWithAnime`).

## Example

```kotlin
package fr.shikkanime.models

import java.util.UUID

data class Anime(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null
)

data class Episode(
    val id: UUID = UUID.randomUUID(),
    val animeId: UUID,
    val title: String,
    val number: Int
)

data class EpisodeWithAnime(
    val episode: Episode,
    val anime: Anime
)
```
