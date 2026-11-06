import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/format/labels.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_localization_model.dart';

/// One translation of an anime: its locale, title and description.
class AnimeLocalizationBlock extends StatelessWidget {
  const AnimeLocalizationBlock({super.key, required this.localization});

  final AnimeLocalizationModel localization;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              LocaleBadge(locale: localeLabel(localization.locale)),
              const SizedBox(width: 8),
              Expanded(
                child: SelectableText(
                  localization.title ?? '—',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          SelectableText(
            localization.description?.isNotEmpty == true
                ? localization.description!
                : localizations.noDescription,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
