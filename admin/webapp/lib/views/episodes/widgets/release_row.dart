import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/latest_episode_model.dart';

/// One episode of the release list: its main title, then the anime, identity and date.
class ReleaseRow extends StatelessWidget {
  const ReleaseRow({super.key, required this.episode, required this.onTap});

  /// Episode displayed by the row.
  final LatestEpisodeModel episode;

  /// Opens the episode under its anime.
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
      episode.animeTitle,
      identity,
      formatDateTime(episode.releaseDateTime),
      localizations.releaseCount(episode.releaseCount),
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
