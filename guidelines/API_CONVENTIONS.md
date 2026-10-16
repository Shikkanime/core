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
- **One DTO for both transports.** REST controllers and GraphQL queries return the very same DTO
  type: a GraphQL-specific duplicate DTO is forbidden. GraphQL queries live in
  `graphql/queries/`, DTOs stay in `dtos/`.
- **A mapping is an extension function on the source model, declared in the DTO file it builds:**
  `fun Anime.toDto(): AnimeDto`, `fun AnimeLocalization.toDto(): AnimeLocalizationDto` — never a
  companion factory, never a top-level function without receiver.
- **Map lists with method references, never with lambdas:** `animes.map(Anime::toDto)`.
- **Error responses use `MessageDto.error(...)`** (framework `fr.shikkanime.ktor.dtos`), never a raw
  `String`; the OpenAPI response type is `ErrorMessageDto::class`.
- **A controller parameter is typed with its real domain type, never with `String` to re-parse it:**
  `@PathParam("id") id: Uuid`. The framework resolver converts textual parameters and answers an
  HTTP 400 carrying a `MessageDto` error when the value does not match the expected type.

```kotlin
@Serializable
data class AnimeDto(
    val id: String,
    val localizations: List<AnimeLocalizationDto>
)

fun Anime.toDto(): AnimeDto =
    AnimeDto(
        id = id.toString(),
        localizations = localizations.map(AnimeLocalization::toDto)
    )
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
