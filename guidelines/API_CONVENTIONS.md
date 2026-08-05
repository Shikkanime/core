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
