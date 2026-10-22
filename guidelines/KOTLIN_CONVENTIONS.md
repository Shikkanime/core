# Kotlin Conventions

- **Use expression bodies with `=`** for simple functions, placing the expression on the line
  following the `=`.
- **Keep return types explicit.**
- **Keep a condition of up to three operands on a single line:**
  `if (storedSortIndex != null && storedSortIndex < candidateSortIndex)`, and the same for a boolean
  value: `val hasChanged = response.hasChanged || country !in cachedEpisodesByCountry`.
- **Past three operands, place one operand per line,** prefixed by its operator, and move the whole
  condition into a dedicated function named after what it checks: `if (isEpisodeOutOfRange(episode))`.
  A condition of three operands or fewer that no longer fits 110 characters is extracted the same way.
- **Break chained function calls** so that each call after the first one is placed on a new indented line.
- **`if` and `else` branches must always be placed on separate lines.** Never write `if` and `else`
  together on a single line.
- **Curly braces `{}` are not required for `if` statements.** For single-line statements or
  expressions, omit braces `{}`. Keep `if`, `else`, and their branch expressions on separate lines.
- **Dynamic property accessors (`get()` and `set()`) must always be on new lines.** Never declare
  `get()` or `set()` inline on the same line as the variable declaration.
- **Nested classes (other than test classes) must be placed at the very beginning of the parent class.**
  Always declare inner and nested data classes, sealed classes, or helper types at the top of the parent
  class before declaring properties or functions.
- **Never add KDoc on variables or properties.** Variables, constants, and fields must have
  self-explanatory names without KDoc comments.
- **Import every type you use; never write a fully-qualified name inline.** `import kotlin.uuid.Uuid`
  and `Uuid.parse(id)`, never `kotlin.uuid.Uuid.parse(id)`.
- **Name a property in camelCase, a type in PascalCase:** a top-level value reads `uuidScalar`, never
  `UuidScalar`, which would name a type. The same rule covers local variables and parameters.
- **A conversion is a total function of its receiver:** `toModel()` / `toDto()` take NO parameter and
  read only the constructor state of the source. When a mapping needs external state (a platform, an
  owner identifier), the SOURCE misses a constructor parameter — fix the source, never grow the
  conversion signature. Each value is written exactly once by the single holder of that fact: a
  later `copy(...)` re-assigning what a previous step already decided is the smell to eliminate.
- **Map lists with method references, never with lambdas:** `animes.map(Anime::toDto)` instead of
  `animes.map { it.toDto() }`, `map(ImageEntity::toModel)` instead of `map { it.toModel() }`. The
  unqualified `::toDto` form is accepted only when a single `toDto` is in scope; overloaded
  mappings take the class-qualified reference.
- **A conversion function (`toModel`, `toDto`) is an extension function on the source class,
  declared in the file of the type it builds:** `fun Anime.toDto()` lives in `AnimeDto.kt`,
  `PlatformAnime.toModel()` stays a member of `PlatformAnime` (it carries the data and can see its
  target). Never a companion factory, never a wrapper `?.let { entity -> entity.toModel(...) }`.
- **Every non-test method must have complete KDoc.** Must include a descriptive summary, `@param` for
  each parameter, `@return` explaining the return value, and `@see` referencing related classes.
- **A code line stays at or under 110 characters, counted from its first non-whitespace character.**
  Leading indentation is free, so an indented guard keeps its whole budget. Only code is measured:
  strings count, a line holding nothing but a comment or KDoc does not, and neither do Markdown and
  the `guidelines/` documents. Break an over-long line rather than leaving it long, and run
  `tools/check_line_length.py` before committing.

```kotlin
fun isAnimeValid(anime: Anime): Boolean =
    anime.name.isNotBlank() && anime.name.length <= 255

val isValid: Boolean
    get() =
        !video.isPromo && video.order > 0 && video.id > 0

// Four operands: one per line, inside a function named after the check
if (isEpisodeOutOfRange(episode))
    return@mapNotNull null

val withoutSeason = if (season > 0)
    withoutStatic.removeSuffix(" $season")
else
    withoutStatic

if (isThreadCpuTimeSupported && !isThreadCpuTimeEnabled)
    isThreadCpuTimeEnabled = true

if (!video.isValid)
    return@mapNotNull null

val cleanedTitle = if (rawTitle.isNotBlank()) {
    rawTitle.replace(REGEX, "")
        .trim()
} else {
    defaultTitle
}
```
