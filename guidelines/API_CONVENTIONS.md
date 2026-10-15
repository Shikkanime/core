# API and Contract Conventions

Applied to `:api`, `:site`, `:admin` application modules.

## Types and Contracts

- **Prefer immutable `data class` DTOs** with `val` properties for request/response payloads.
- **Keep persistence entities and domain models separate** from API contract DTOs when necessary.
- **Document business rules on interfaces** rather than implementations.

```kotlin
data class AnimeDto(
    val id: UUID,
    val name: String,
    val description: String?
)
```

## DTO Mapping

- **One DTO per file.** A file never holds several DTOs: `AnimeDto.kt` carries `AnimeDto` only.
- **A DTO builds itself from its source through its own `toDto` factory, declared inside the DTO
  class (companion object), never as a top-level extension:** `AnimeDto.toDto(anime)`,
  `AnimeLocalizationDto.toDto(localization)`. The GraphQL views follow the same rule with
  `toGraphQl` (`AnimeGraphQlDto.toGraphQl(anime)`).
- **GraphQL queries and GraphQL DTOs live in separate files** (`AnimeQuery.kt` for the query
  operations, `AnimeGraphQlDto.kt` for the DTOs), exactly like the REST controllers and their DTOs.
- **Map lists with the method reference `::toDto`, never with a lambda** (see KOTLIN_CONVENTIONS.md).

```kotlin
@Serializable
data class AnimeDto(
    val id: Uuid,
    val localizations: List<AnimeLocalizationDto>
) {
    companion object {
        fun toDto(anime: Anime): AnimeDto =
            AnimeDto(
                id = anime.id,
                localizations = anime.localizations.map(::toDto)
            )
    }
}
```

## Controllers and Routes

- **Prefer one return type** per controller or route when possible.
- **Use `ResponseEntity`** when a route needs multiple response types.
- **Every exposed API route should include OpenAPI documentation.**
- **Reuse Ktor modules** (`configureDefaultModules()`, `configureSwaggerRoute()`) provided by `shikkanime-framework:ktor`.

```kotlin
fun Application.configureApiRoutes() {
    routing {
        configureSwaggerRoute("Shikkanime API", "1.0.0")
        get("/api/v1/animes") {
            // Call service/repository and respond with DTO
        }
    }
}
```
