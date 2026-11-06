import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/core/format/labels.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/episode_detail_model.dart';
import 'package:shikkanime_admin/views/episode_detail/widgets/release_block.dart';

/// Cards of one episode: its identity, its translations, its releases and its artwork.
class EpisodeContent extends StatelessWidget {
  const EpisodeContent({super.key, required this.episode});

  final EpisodeDetailModel episode;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                DetailInfoRow(
                  label: localizations.identifier,
                  value: episode.id,
                ),
                DetailInfoRow(
                  label: localizations.animeIdentifier,
                  value: episode.animeId ?? '—',
                ),
                DetailInfoRow(
                  label: localizations.season,
                  value: '${episode.season}',
                ),
                DetailInfoRow(
                  label: localizations.episodeType,
                  value: localizations.episodeTypeName(episode.episodeType),
                ),
                DetailInfoRow(
                  label: localizations.number,
                  value: '${episode.number}',
                ),
                DetailInfoRow(
                  label: localizations.releaseDate,
                  value: formatDateTime(episode.releaseDateTime),
                ),
                DetailInfoRow(
                  label: localizations.duration,
                  value: localizations.durationMinutes(
                    durationInMinutes(episode.duration),
                  ),
                ),
                DetailInfoRow(
                  label: localizations.createdAt,
                  value: formatOptionalDateTime(episode.createdAt),
                ),
                DetailInfoRow(
                  label: localizations.updatedAt,
                  value: formatOptionalDateTime(episode.updatedAt),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                DetailSectionTitle(title: localizations.localizations),
                const SizedBox(height: 8),
                if (episode.localizations.isEmpty)
                  const EmptyDetailValue()
                else
                  for (final localization in episode.localizations)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              LocaleBadge(
                                locale: localeLabel(localization.locale),
                              ),
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
                    ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                DetailSectionTitle(title: localizations.platforms),
                const SizedBox(height: 8),
                if (episode.releases.isEmpty)
                  const EmptyDetailValue()
                else
                  for (final release in episode.releases)
                    ReleaseBlock(release: release),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                DetailSectionTitle(title: localizations.artwork),
                const SizedBox(height: 4),
                Text(
                  localizations.artworkNotDisplayed,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 8),
                if (episode.images.isEmpty)
                  const EmptyDetailValue()
                else
                  for (final image in episode.images)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: SelectableText(
                        '${localizations.imageTypeName(image.type)} — '
                        '${image.url}',
                        style: theme.textTheme.bodySmall,
                      ),
                    ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
