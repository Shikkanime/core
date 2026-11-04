import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/anime_detail_skeleton.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/anime_episodes.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/anime_header.dart';
import 'package:shikkanime_admin/core/widgets/pagination_bar.dart';
import 'package:shikkanime_admin/core/widgets/state_views.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/episode/episode_summary_model.dart';
import 'package:shikkanime_admin/router/app_router.dart';
import 'package:shikkanime_admin/viewmodels/anime_detail_view_model.dart';

/// Detail screen of one anime: everything stored about it, and its paged episodes.
class AnimeDetailView extends StatefulWidget {
  const AnimeDetailView({super.key});

  @override
  State<AnimeDetailView> createState() => _AnimeDetailViewState();
}

class _AnimeDetailViewState extends State<AnimeDetailView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => context.read<AnimeDetailViewModel>().load(),
    );
  }

  void _openEpisode(EpisodeSummaryModel episode) {
    context.go(
      Routes.episode(context.read<AnimeDetailViewModel>().animeId, episode.id),
      extra: episode.title,
    );
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final viewModel = context.watch<AnimeDetailViewModel>();

    return Scaffold(
      appBar: AppBar(
        title: Text(viewModel.title),
        actions: [
          IconButton(
            onPressed: viewModel.isAnimeLoading ? null : viewModel.load,
            tooltip: localizations.refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              children: [
                if (viewModel.isAnimeLoading && viewModel.anime == null)
                  const AnimeDetailSkeleton()
                else if (viewModel.animeError != null &&
                    viewModel.anime == null)
                  ErrorStateView(
                    message: viewModel.animeError,
                    onRetry: viewModel.load,
                  )
                else if (viewModel.anime != null)
                  AnimeHeader(anime: viewModel.anime!),
                const SizedBox(height: 24),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        localizations.episodes,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ),
                    Text(
                      localizations.episodeCount(viewModel.totalElements),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                AnimeEpisodes(viewModel: viewModel, onOpen: _openEpisode),
              ],
            ),
          ),
          PaginationBar(
            page: viewModel.page,
            totalPages: viewModel.totalPages,
            itemLabel: localizations.episodeCount(viewModel.totalElements),
            onFirst: () => viewModel.goToPage(1),
            onPrevious: () => viewModel.goToPage(viewModel.page - 1),
            onNext: () => viewModel.goToPage(viewModel.page + 1),
            onLast: () => viewModel.goToPage(viewModel.totalPages),
            onGoToPage: viewModel.goToPage,
          ),
        ],
      ),
    );
  }
}
