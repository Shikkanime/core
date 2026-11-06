import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';

/// One episode of an anime, as the compact list displays it.
class EpisodeRow extends StatelessWidget {
  const EpisodeRow({super.key, required this.episode, required this.onTap});

  final EpisodeSummaryModel episode;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;
    final identity = localizations.episodeIdentity(
      episode.season,
      localizations.episodeTypeName(episode.episodeType),
      episode.number,
    );
    final metadata = [
      if (episode.title != null) identity,
      formatDateTime(episode.releaseDateTime),
      localizations.durationMinutes(durationInMinutes(episode.duration)),
      localizations.releaseCount(episode.releaseCount),
      localizations.localizationCount(episode.localizationCount),
    ].join('  ·  ');

    return ListTile(
      dense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16),
      title: Text(
        episode.title ?? identity,
        style: theme.textTheme.bodyLarge,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        metadata,
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: Icon(
        Icons.chevron_right,
        size: 18,
        color: theme.colorScheme.onSurfaceVariant,
      ),
      onTap: onTap,
    );
  }
}
