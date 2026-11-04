import 'package:flutter/material.dart';
import 'package:shikkanime_admin/core/widgets/state_views.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/viewmodels/anime_list_view_model.dart';
import 'package:shikkanime_admin/views/anime_list/widgets/anime_list_skeleton.dart';
import 'package:shikkanime_admin/views/anime_list/widgets/anime_row.dart';

/// Body of the catalog screen: skeleton, failure, empty state or the paged rows.
class AnimeListBody extends StatelessWidget {
  const AnimeListBody({
    super.key,
    required this.viewModel,
    required this.onOpen,
  });

  /// State of the catalog page displayed.
  final AnimeListViewModel viewModel;

  /// Opens the anime the administrator selected.
  final ValueChanged<AnimeSummaryModel> onOpen;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;

    if (viewModel.isLoading && viewModel.animes.isEmpty) {
      return const AnimeListSkeleton();
    }

    if (viewModel.error != null && viewModel.animes.isEmpty) {
      return ErrorStateView(message: viewModel.error, onRetry: viewModel.load);
    }

    if (viewModel.animes.isEmpty) {
      return EmptyStateView(
        message: viewModel.query.isEmpty
            ? localizations.noAnime
            : localizations.noResult,
      );
    }

    return Column(
      children: [
        if (viewModel.isLoading) const LinearProgressIndicator(minHeight: 2),
        Expanded(
          child: ListView.separated(
            itemCount: viewModel.animes.length,
            separatorBuilder: (context, index) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final anime = viewModel.animes[index];

              return AnimeRow(anime: anime, onTap: () => onOpen(anime));
            },
          ),
        ),
      ],
    );
  }
}
