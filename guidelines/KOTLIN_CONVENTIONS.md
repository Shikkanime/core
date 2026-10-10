# Kotlin Conventions

- **Use expression bodies with `=`** for simple functions, placing the expression on the line
  following the `=`.
- **Keep return types explicit.**
- **Format multi-operator conditions (`&&`, `||`)** with each condition on its own line,
  prefixed by the operator. If the total condition is complex or long, extract it into a dedicated
  function for readability.
- **Break chained function calls** so that each call after the first one is placed on a new indented line.
- **Do not comment global constants or enum entries.** Add comments to properties only when they are dynamic
  (computed via custom getters `get()` or `by lazy`).
- **Do not document external API DTO constructors or serialized fields.** Only dynamic properties and
  conversion functions within DTOs require comments/KDoc.
- **Extract long chains or accessors** into named functions and add **KDoc** documenting the business rule.
- **Maximum line length is 110 characters** (including comments, KDoc, and strings).

```kotlin
fun isAnimeValid(anime: Anime): Boolean =
    anime.name.isNotBlank()
        && anime.name.length <= 255

val isValid = !video.isPromo
    && video.order > 0
    && video.id > 0

val cleanedTitle = rawTitle.replace(REGEX, "")
    .trim()
```
