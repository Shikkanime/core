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

### Variables and Properties

- **Never put KDoc on variables or properties.** Do not document global constants, enum entries,
  local variables, class fields, or constructor properties with KDoc. Names must be self-explanatory.
- **Do not document external API DTO constructors or serialized fields with KDoc.** DTOs mapping remote
  JSON payloads (such as ADN API models) should not have constructor or property tags.

### Methods and Functions

- **Every non-test function and method must have complete KDoc.**
  The documentation must include a concise description of purpose, explicit `@param` tags for every
  parameter, an `@return` tag describing the result, and `@see` tags referencing relevant classes
  using markdown links (e.g. `[PlatformEpisode]`, `[Country]`).

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
