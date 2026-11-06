import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/format/formatters.dart';
import 'package:shikkanime_admin/core/format/labels.dart';
import 'package:shikkanime_admin/core/widgets/detail_widgets.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/episode_release_model.dart';

/// One platform release of an episode.
class ReleaseBlock extends StatelessWidget {
  const ReleaseBlock({super.key, required this.release});

  final EpisodeReleaseModel release;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final localizations = AppLocalizations.of(context)!;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '${localizations.platformName(release.platform)} · '
                  '${localeLabel(release.audioLocale)}',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              if (release.uncensored)
                Chip(
                  visualDensity: VisualDensity.compact,
                  label: Text(localizations.uncensored),
                ),
            ],
          ),
          const SizedBox(height: 2),
          Text(
            formatDateTime(release.releaseDateTime),
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 2),
          DetailInfoRow(
            label: localizations.platformId,
            value: release.platformId,
          ),
          SelectableText(release.url, style: theme.textTheme.bodySmall),
        ],
      ),
    );
  }
}

/// Placeholder of the episode cards while the detail loads.
