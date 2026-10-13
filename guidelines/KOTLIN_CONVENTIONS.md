# Kotlin Conventions

- **Use expression bodies with `=`** for simple functions, placing the expression on the line
  following the `=`.
- **Keep return types explicit.**
- **Format multi-operator conditions (`&&`, `||`)** with each condition on its own line,
  prefixed by the operator. If the total condition is complex or long, extract it into a dedicated
  function for readability.
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
- **Every non-test method must have complete KDoc.** Must include a descriptive summary, `@param` for
  each parameter, `@return` explaining the return value, and `@see` referencing related classes.
- **Maximum line length is 110 characters** (including comments, KDoc, and strings).

```kotlin
fun isAnimeValid(anime: Anime): Boolean =
    anime.name.isNotBlank()
        && anime.name.length <= 255

val isValid: Boolean
    get() =
        !video.isPromo
            && video.order > 0
            && video.id > 0

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
