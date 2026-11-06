import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/core/widgets/state_views.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/views/episode_detail/widgets/episode_content.dart';
import 'package:shikkanime_admin/views/episode_detail/widgets/episode_detail_skeleton.dart';
import 'package:shikkanime_admin/viewmodels/episode_detail_view_model.dart';

/// Detail screen of one episode: everything stored about it.
class EpisodeDetailView extends StatefulWidget {
  const EpisodeDetailView({
    super.key,
    required this.episodeId,
    this.initialTitle,
  });

  final String episodeId;
  final String? initialTitle;

  @override
  State<EpisodeDetailView> createState() => _EpisodeDetailViewState();
}

class _EpisodeDetailViewState extends State<EpisodeDetailView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => context.read<EpisodeDetailViewModel>().load(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final viewModel = context.watch<EpisodeDetailViewModel>();
    final episode = viewModel.episode;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          episode?.displayTitle ??
              widget.initialTitle ??
              localizations.episodeDetails,
        ),
        actions: [
          IconButton(
            onPressed: viewModel.isLoading ? null : viewModel.load,
            tooltip: localizations.refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: switch (episode) {
        null when viewModel.isLoading => const EpisodeDetailSkeleton(),
        null when viewModel.error != null => ErrorStateView(
          message: viewModel.error,
          onRetry: viewModel.load,
        ),
        null => const SizedBox.shrink(),
        final value => EpisodeContent(episode: value),
      },
    );
  }
}
