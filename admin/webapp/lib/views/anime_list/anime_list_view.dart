import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/core/widgets/pagination_bar.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/router/app_router.dart';
import 'package:shikkanime_admin/viewmodels/anime_list_view_model.dart';
import 'package:shikkanime_admin/views/anime_list/widgets/anime_list_body.dart';

/// Catalog screen: the stored animes, one page at a time, searchable by title.
///
/// The same screen serves the whole catalog and one platform: [platform] only changes the title
/// and the filter the ViewModel sends, the rows and the paginator are identical.
class AnimeListView extends StatefulWidget {
  const AnimeListView({super.key, this.platform});

  final String? platform;

  @override
  State<AnimeListView> createState() => _AnimeListViewState();
}

class _AnimeListViewState extends State<AnimeListView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => context.read<AnimeListViewModel>().load(),
    );
  }

  late final _searchController = TextEditingController(
    text: context.read<AnimeListViewModel>().query,
  );

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _open(AnimeSummaryModel anime) {
    context.go(Routes.anime(anime.id), extra: anime);
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final viewModel = context.watch<AnimeListViewModel>();

    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.platform == null
              ? localizations.appTitle
              : localizations.platformAnimes(
                  localizations.platformName(widget.platform!),
                ),
        ),
        actions: [
          IconButton(
            onPressed: viewModel.isLoading ? null : viewModel.load,
            tooltip: localizations.refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: TextField(
              controller: _searchController,
              onChanged: viewModel.search,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.search),
                hintText: localizations.searchAnime,
                border: const OutlineInputBorder(),
                isDense: true,
              ),
            ),
          ),
          Expanded(
            child: AnimeListBody(viewModel: viewModel, onOpen: _open),
          ),
          PaginationBar(
            page: viewModel.page,
            totalPages: viewModel.totalPages,
            itemLabel: localizations.animeCount(viewModel.total),
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
