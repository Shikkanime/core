import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/router/app_router.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/localization_block.dart';

/// Card of one anime: its identity, audit columns, platforms, translations and artwork.
class AnimeHeader extends StatelessWidget {
  const AnimeHeader({super.key, required this.anime});

  final AnimeDetailModel anime;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailInfoRow(label: localizations.identifier, value: anime.id),
            DetailInfoRow(
              label: localizations.createdAt,
              value: formatOptionalDateTime(anime.createdAt),
            ),
            DetailInfoRow(
              label: localizations.updatedAt,
              value: formatOptionalDateTime(anime.updatedAt),
            ),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.platforms),
            const SizedBox(height: 4),
            if (anime.platforms.isEmpty)
              const EmptyDetailValue()
            else
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  for (final platform in anime.platforms)
                    ActionChip(
                      visualDensity: VisualDensity.compact,
                      onPressed: () =>
                          context.go(Routes.platform(platform.platform)),
                      label: Text(
                        '${localizations.platformName(platform.platform)} '
                        '#${platform.platformId}',
                      ),
                    ),
                ],
              ),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.localizations),
            const SizedBox(height: 4),
            if (anime.localizations.isEmpty)
              const EmptyDetailValue()
            else
              for (final localization in anime.localizations)
                AnimeLocalizationBlock(localization: localization),
            const Divider(height: 24),
            DetailSectionTitle(title: localizations.artwork),
            const SizedBox(height: 4),
            Text(
              localizations.artworkNotDisplayed,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            if (anime.images.isEmpty)
              const EmptyDetailValue()
            else
              for (final image in anime.images)
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: SelectableText(
                    '${localizations.imageTypeName(image.type)} — ${image.url}',
                    style: theme.textTheme.bodySmall,
                  ),
                ),
          ],
        ),
      ),
    );
  }
}
