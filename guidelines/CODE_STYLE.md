# Code Style Guide

## General Style

- **Write all code, examples, and comments in English.**
- **Prefer short, readable, direct code** with explicit names.
- **Keep one clear responsibility** per class or function.
- **Keep comments rare, useful, and focused** on non-obvious behavior.
- **Prefer line breaks** that keep lines short and readable (maximum 110 characters).
- **Reuse existing project patterns** instead of introducing new ones without need.

## Comments

When calling a **service**, **repository**, or **port**, add a brief comment explaining
the **business reason** for the call. The comment should explain the **intent**, not the technical mechanism.

```kotlin
// Load cached anime details before triggering episode fetch
val anime = animeRepository.findAnimeById(animeId)
```

### Variables and Properties

- **Do not comment global constants or enum entries.** Names must be self-explanatory.
- **Do not document external API DTO constructors or serialized fields with KDoc.** DTOs mapping remote
  JSON payloads (such as ADN API models) should not have constructor or property tags.
- **Add comments only to dynamic properties** (properties computed with custom getters `get()`
  or `by lazy` delegates) and conversion functions.
