import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shikkanime_admin/core/widgets/state_views.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/router/app_router.dart';
import 'package:shikkanime_admin/viewmodels/latest_episodes_view_model.dart';
import 'package:shikkanime_admin/views/episodes/widgets/episodes_skeleton.dart';
import 'package:shikkanime_admin/views/episodes/widgets/release_row.dart';

/// Body of the episode screen: skeleton, failure, empty state or the paged rows.
class EpisodesBody extends StatelessWidget {
  const EpisodesBody({super.key, required this.viewModel});

  /// State of the release list displayed.
  final LatestEpisodesViewModel viewModel;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    if (viewModel.isLoading && viewModel.episodes.isEmpty) {
      return const EpisodesSkeleton();
    }

    if (viewModel.error != null && viewModel.episodes.isEmpty) {
      return ErrorStateView(message: viewModel.error, onRetry: viewModel.load);
    }

    if (viewModel.episodes.isEmpty) {
      return EmptyStateView(message: localizations.noEpisodeStored);
    }

    return Column(
      children: [
        if (viewModel.isLoading) const LinearProgressIndicator(minHeight: 2),
        Expanded(
          child: ListView.separated(
            itemCount: viewModel.episodes.length,
            separatorBuilder: (context, index) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final episode = viewModel.episodes[index];

              return ReleaseRow(
                episode: episode,
                onTap: () => context.go(
                  Routes.episode(episode.animeId, episode.id),
                  extra: episode.title,
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
