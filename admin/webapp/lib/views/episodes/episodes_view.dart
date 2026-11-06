import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/core/widgets/pagination_bar.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/viewmodels/latest_episodes_view_model.dart';
import 'package:shikkanime_admin/views/episodes/widgets/episodes_body.dart';

/// Screen of every stored episode, newest release first.
///
/// The sort is a server concern: the list asks for one page ordered by the broadcast date of the
/// episode, newest first, instead of loading the table to sort it. Each row opens the episode
/// under its anime, so the back button returns to the list.
class LatestEpisodesView extends StatefulWidget {
  const LatestEpisodesView({super.key});

  @override
  State<LatestEpisodesView> createState() => _LatestEpisodesViewState();
}

class _LatestEpisodesViewState extends State<LatestEpisodesView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => context.read<LatestEpisodesViewModel>().load(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final viewModel = context.watch<LatestEpisodesViewModel>();

    return Scaffold(
      appBar: AppBar(title: Text(localizations.episodes)),
      body: Column(
        children: [
          Expanded(child: EpisodesBody(viewModel: viewModel)),
          PaginationBar(
            page: viewModel.page,
            totalPages: viewModel.totalPages,
            itemLabel: localizations.episodeCount(viewModel.total),
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
