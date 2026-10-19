# Models Module Guidelines

The `:models` module represents the core domain objects shared across all modules.

## Principles

1. **Zero Framework Dependencies**: No ORM annotations, no Ktor imports, no Jackson/serialization annotations unless explicitly shared.
2. **Immutability**: Always use `data class` with `val` properties.
3. **Reference by ID**: Avoid deep object nesting in base domain models. Reference related entities by `Uuid` (e.g., `animeId: Uuid`).
4. **Projections for Composites**: When an application requires an aggregated view, define a projection class (e.g., `EpisodeWithAnime`).
5. **A model built outside the persistence carries no identity**: `id`, `createdAt` and `updatedAt`
   are nullable and left at `null` by the builders (`Platform*.toModel()`), because the row does not
   exist yet and inventing values only gets overwritten at save. The persistence fills them, and a
   known identity is re-applied through `copy(id = ...)` on update.

## Example

```kotlin
package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class Anime(
    val id: Uuid? = null,
    val name: String,
    val description: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class Episode(
    val id: Uuid? = null,
    val animeId: Uuid,
    val title: String,
    val number: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class EpisodeWithAnime(
    val episode: Episode,
    val anime: Anime
)
```
