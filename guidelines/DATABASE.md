# Database Module Guidelines

The `:database` module encapsulates all persistence logic using Exposed and `DatabaseWrapper`.

## Entity & Table Encapsulation

- All Exposed tables (`UUIDTable`) and entities (`UUIDEntity`) **must be declared `internal`**.
- Entities **must never be returned** by public repository methods. Public methods must return domain models from `:models`.

```kotlin
// Internal table
internal object AnimeTable : UUIDTable("animes") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
}

// Internal entity
internal class AnimeEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AnimeEntity>(AnimeTable)

    var name by AnimeTable.name
    var description by AnimeTable.description

    fun toModel(): Anime = Anime(id = id.value, name = name, description = description)
}
```

## Writing Repositories

Repositories encapsulate queries and entity mapping.

- **Chain the mapping directly on the lookup:** `findById(id)?.toModel(...)` — never wrap a mapping
  in `?.let { entity -> entity.toModel(...) }` and never hold it in an intermediate variable. The
  lookup and the mapping read as one chain. The single accepted wrapper shape is a mapping that
  needs the matched row's own keys to resolve its children (`?.let { it.toModel(findBy(it.id)) }`):
  there, the wrapper is the chain.
- **A lookup by a column of a joined table joins, it never walks:** `AnimeEntity.wrapRows(join)`
  reads the anime in one round trip; finding the link rows and then re-fetching each target is an
  N+1 (see [PERFORMANCE.md](./PERFORMANCE.md)).

### Custom "Upsert" DSL (`EntityRepository`)

Internal repositories extend `EntityRepository<UUID, ENTITY>` which provides standard methods (`findById`, `findAllByIds`) and an upsert DSL: `ifExists`, `newIfNotExists`, and `applyFlush`.

```kotlin
internal class AnimeInternalRepository : EntityRepository<UUID, AnimeEntity>(AnimeEntity)

class AnimeRepository {
    private val repo = AnimeInternalRepository()

    fun findAnimeById(id: UUID): Anime? =
        repo.findById(id)?.toModel()

    fun saveAnime(anime: Anime): Anime {
        val existing = AnimeEntity.findById(anime.id)
        val entity = if (existing != null) {
            existing.apply {
                name = anime.name
                description = anime.description
            }
        } else {
            AnimeEntity.new(anime.id) {
                name = anime.name
                description = anime.description
            }
        }
        return entity.toModel()
    }
}
```

- Use `services/` (e.g. `SiteQueryService`, `AdminQueryService`) for module-specific complex queries.
