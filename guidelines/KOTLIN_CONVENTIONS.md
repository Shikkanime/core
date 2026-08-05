# Kotlin Conventions

- **Use expression bodies with `=`** for simple functions.
- **Keep return types explicit.**
- **Keep multi-line logic** when it improves readability.
- **Extract long chains of calls or accessors** into named functions when that makes the intent clearer, and add **KDoc** when it documents a business rule.

```kotlin
fun isAnimeValid(anime: Anime): Boolean =
    anime.name.isNotBlank() && anime.name.length <= 255
```
