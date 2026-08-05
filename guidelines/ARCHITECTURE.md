# Architecture Guide

Respect the multi-module project layers and responsibilities:

```text
               +-----------------------------------+
               |              :models              |
               | (Pure Domain Objects & Enums)     |
               +-----------------+-----------------+
                                 ^
                                 |
               +-----------------+-----------------+
               |             :database             |
               | (Exposed Entities, Tables, Repos) |
               +-----------------+-----------------+
                                 ^
        +----------------+-------+--------+----------------+
        |                |                |                |
  +-----+----+    +------+---+    +-------+--+    +--------+--+
  |   :api   |    |  :site   |    |  :admin  |    |  :jobs    |
  |  (Ktor)  |    |  (Web)   |    |  (Admin) |    |  (Worker) |
  +----------+    +----------+    +----------+    +-----------+
```

## Module Responsibilities

### `:models` (Domain Layer)
- Pure Kotlin module with **zero external framework dependencies**.
- Contains domain models (`Anime`, `Episode`, `Metadata`), domain enums (`LangType`, `PlatformType`), and projection classes.
- Models must be immutable (`data class` with `val`).

### `:database` (Persistence Layer)
- Manages database access using Exposed and `DatabaseWrapper`.
- Defines Exposed tables (`UUIDTable`) and entities (`UUIDEntity`).
- Entities and tables **must be `internal`** to `:database` and **never leak** to application modules.
- Repositories extend `AbstractRepository` and expose functions returning pure `:models` domain objects.

### Application Modules (`:api`, `:site`, `:admin`, `:jobs`)
- Independent application entry points, runnable in separate Docker containers.
- Depend on `:models` and `:database`.
- **Must remain independent of each other** (e.g., `:api` does not depend on `:site` or `:admin`).
- **Controllers / Routes** expose HTTP endpoints and delegate business logic to services.
- **Services** orchestrate business rules.

## Core Technologies
- **Language**: Kotlin (JVM Toolchain 21)
- **Frameworks**: `shikkanime-framework` (`core`, `exposed`, `ktor`, `validator`)
- **Web**: Ktor (CIO engine), Swagger / OpenAPI
- **Database**: Exposed, HikariCP, H2 / PostgreSQL
- **Build System**: Gradle Kotlin DSL with Version Catalog
