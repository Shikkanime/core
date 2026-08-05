# Code Style Guide

## General Style

- **Write all code, examples, and comments in English.**
- **Prefer short, readable, direct code** with explicit names.
- **Keep one clear responsibility** per class or function.
- **Keep comments rare, useful, and focused** on non-obvious behavior.
- **Prefer line breaks** that keep lines short and readable.
- **Reuse existing project patterns** instead of introducing new ones without need.

## Comments

When calling a **service**, **repository**, or **port**, add a brief comment explaining the **business reason** for the call. The comment should explain the **intent**, not the technical mechanism.

```kotlin
// Load cached anime details before triggering episode fetch
val anime = animeRepository.findAnimeById(animeId)
```
