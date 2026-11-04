# Code style

The rules below come from review: they are what a change is judged on, not suggestions.

## One widget per file

- A `StatelessWidget` lives in its own file: a screen in `views/<screen>/<screen>_view.dart`, its
  row and blocks in `views/<screen>/widgets/<block>.dart` or `lib/core/widgets/<widget>.dart` when
  another screen reuses them.
- A `StatefulWidget` may sit next to the private widgets that only its state uses, because those
  widgets read the state the file owns.
- A file never exports two public widgets.

## Stateless first

A widget is `StatelessWidget` unless it owns something a rebuild cannot recreate: a
`TextEditingController`, a `ScrollController`, a `TabController`, a focus node, a subscription or a
timer. The state lives in the ViewModel; the widget reads it and forwards events. "I need to call
`setState` to show a dialog" is not a reason to make a screen stateful.

## A readable `build`

- A `build` method stays under **200-250 lines**. Beyond that, the blocks become named widgets
  (`_Header`, `_Metadata`, `_Section`) in their own files when they are stateless, and the build
  reads as the composition of those blocks.
- Extract by responsibility, not by line count: a block that fetches nothing, holds no state and
  renders one part of the screen is a widget.
- No widget is built inline in a `build` when it is reused by another screen.

## Loading, empty and error states

- A loading screen shows the skeleton of the content it will display, never a bare
  `CircularProgressIndicator`: `lib/core/widgets/skeleton.dart` (the `Shimmer`/`AppSkeleton` widgets
  of `Shikkanime/application` v2) gives the layout a stable shape while the data arrives.
- A refresh of an already displayed list keeps the rows and shows a thin progress line.
- An empty or failed screen uses `EmptyStateView` / `ErrorStateView`, never ad-hoc text.

## Text and labels

- Every user-visible string comes from `AppLocalizations`. A label that depends on a server enum
  (`platform`, `locale`, `episodeType`, `imageType`) is a **`select` in the ARB file**, not a
  `switch` in Dart:

```json
"platformName": "{platform, select, CRUNCHYROLL{Crunchyroll} ADN{ADN} NETFLIX{Netflix} other{{platform}}}",
"@platformName": { "placeholders": { "platform": { "type": "String" } } }
```

```dart
Text(localizations.platformName(platform))
```

  The ARB is the single place a label changes; a new platform answers `other{...}` and never breaks
  the screen. `lib/core/format/labels.dart` keeps only what an ARB cannot express (a value derived
  from several fields).
- Plurals use the ICU `plural` form, never a ternary on a count.

## Documentation

- Every public class, method and property carries a Dart doc comment (`///`), in English: what it
  does, what it returns, and the constraint a caller must respect. A private helper gets a comment
  only when its intent is not obvious.
- A doc comment says *why* a thing exists, not *what* the code already says.

## Theme

- No colour, radius, padding or font is hardcoded in a widget: they come from `Theme.of(context)` or
  `AppTheme`. `lib/core/theme/app_theme.dart` is the only file that names a `Color`.
