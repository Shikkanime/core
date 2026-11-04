import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/widgets/state_views.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';
import 'package:shikkanime_admin/viewmodels/anime_detail_view_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/episode_row.dart';

/// Episodes of an anime, with their loading, failure and empty states.
class AnimeEpisodes extends StatelessWidget {
  const AnimeEpisodes({
    super.key,
    required this.viewModel,
    required this.onOpen,
  });

  final AnimeDetailViewModel viewModel;
  final ValueChanged<EpisodeSummaryModel> onOpen;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    if (viewModel.isEpisodesLoading && viewModel.episodes.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (viewModel.episodesError != null && viewModel.episodes.isEmpty) {
      return ErrorStateView(
        message: viewModel.episodesError,
        onRetry: () => viewModel.loadEpisodes(viewModel.page),
      );
    }

    if (viewModel.episodes.isEmpty) {
      return EmptyStateView(message: localizations.noEpisode);
    }

    return Column(
      children: [
        if (viewModel.isEpisodesLoading)
          const LinearProgressIndicator(minHeight: 2),
        for (final episode in viewModel.episodes) ...[
          EpisodeRow(episode: episode, onTap: () => onOpen(episode)),
          const Divider(height: 1),
        ],
      ],
    );
  }
}

/// Placeholder of the anime card while the detail loads.
