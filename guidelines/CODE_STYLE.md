# Code Style Guide

## General Style

- **Write all code, examples, and comments in English.**
- **Prefer short, readable, direct code** with explicit names.
- **Keep one clear responsibility** per class or function.
- **Keep comments rare, useful, and focused** on non-obvious behavior.
- **Prefer line breaks** that keep lines short and readable (maximum 110 characters).
- **Reuse existing project patterns** instead of introducing new ones without need.

## Comments and KDoc

When calling a **service**, **repository**, or **port**, add a brief comment explaining
the **business reason** for the call. The comment should explain the **intent**, not the technical mechanism.

```kotlin
// Load cached anime details before triggering episode fetch
val anime = animeRepository.findAnimeById(animeId)
```

- **Do not repeat an inherited contract in KDoc.** An override implementing an interface or
  supertype contract carries no KDoc of its own: the contract is already documented at the
  declaration site. Document the override only when it diverges from the inherited contract — a
  specialized overload, extra state composed from elsewhere, or a documented exception like
  `AnimeEntity.toModel()` returning an aggregate without artwork.

### Variables and Properties

- **A declaration with a single annotation keeps everything on one line, like a regular
  declaration:** `@MockK private lateinit var imageRepository: ImageRepository`. Consecutive
  declarations bearing the SAME annotation are packed together like regular variables, with no
  blank line between them; declarations bearing DIFFERENT annotations form separate groups
  separated by a blank line (the `double saut de ligne`), as in:
  ```kotlin
  @MockK private lateinit var animeRepository: AnimeRepository
  @MockK private lateinit var localizationRepository: AnimeLocalizationRepository
  @MockK private lateinit var platformRepository: AnimePlatformRepository
  @MockK private lateinit var imageRepository: ImageRepository

  @InjectMockKs private lateinit var animeService: AnimeServiceImpl
  ```
- **A declaration with several annotations stacks them, one per line above the declaration,** and
  keeps its blank-line separation:
  ```kotlin
  @MockK
  @Valid
  private val a: A = A()
  ```
  This applies to tests and to all code in general.

- **Never put KDoc on variables or properties.** Do not document global constants, enum entries,
  local variables, class fields, or constructor properties with KDoc. Names must be self-explanatory.
- **Do not document external API DTO constructors or serialized fields with KDoc.** DTOs mapping remote
  JSON payloads (such as ADN API models) should not have constructor or property tags.

### Methods and Functions

- **Every non-test function and method must have complete KDoc.**
  The documentation must include a concise description of purpose, explicit `@param` tags for every
  parameter, a `@return` tag describing the result, and `@see` tags referencing relevant classes
  using markdown links (e.g. `[PlatformEpisode]`, `[Country]`).

### Declaration order in repositories and services

Applies to every repository and service, interface and implementation alike. Declare members in
this order:

1. Read functions returning lists (`findAll...`), first.
2. Unique read functions (`findById...`, `findModelById...`), next.
3. Save and update functions (`save`, `update`, `persist`), after the reads.
4. Delete functions, last, when there are any.

Private helpers back a public function: place them directly above the function they serve, not in
a separate block.

### Naming of composite-key lookups

A lookup function keyed on several criteria is named after the criteria it takes, joined by
`And`, so the signature can be guessed from the name. The identifier criterion is `Id`:
`findByIdAndType`, `findByIdAndLocale`, `findByIdPlatformAndPlatformId`.

## Control Flow and Layout

- **`if` and `else` branches must always be placed on separate lines.**
  Never put single-line `if (...) ... else ...` on a single line.
- **Curly braces `{}` are not required for `if` statements.**
  When an `if` or `if/else` block consists of a single statement or expression, omit the curly braces `{}`.
  Always place the body on a separate indented line following the `if` or `else`.
- **Dynamic property accessors (`get()` and `set()`) must always be on new lines.**
  Never declare `get()` or `set()` inline on the same line as the property signature.
- **Nested classes (other than test classes) must be placed at the very beginning of the parent class.**
  Place nested/inner classes, types, or baseline holders at the top of the parent class body,
  before properties and methods.
