import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/models/anime/anime_summary_model.dart';
import 'package:shikkanime_admin/repositories/anime_repository.dart';
import 'package:shikkanime_admin/repositories/episode_repository.dart';
import 'package:shikkanime_admin/viewmodels/anime_detail_view_model.dart';
import 'package:shikkanime_admin/viewmodels/anime_list_view_model.dart';
import 'package:shikkanime_admin/viewmodels/episode_detail_view_model.dart';
import 'package:shikkanime_admin/viewmodels/latest_episodes_view_model.dart';
import 'package:shikkanime_admin/views/anime_detail/anime_detail_view.dart';
import 'package:shikkanime_admin/views/anime_list/anime_list_view.dart';
import 'package:shikkanime_admin/views/episode_detail/episode_detail_view.dart';
import 'package:shikkanime_admin/views/episodes/episodes_view.dart';
import 'package:shikkanime_admin/views/settings/settings_view.dart';
import 'package:shikkanime_admin/views/shell/admin_shell.dart';

/// Address of the administration screens.
///
/// Every screen owns a URL, so the browser history, a reload and a shared link all reach the same
/// view: an anime is `/animes/<id>` and its episodes live under it.
abstract final class Routes {
  static const animes = '/animes';
  static const episodes = '/episodes';
  static const settings = '/settings';

  static String anime(String id) => '$animes/$id';

  static String episode(String animeId, String episodeId) =>
      '${anime(animeId)}/episodes/$episodeId';
}

/// Builds the router of the administration.
///
/// The list screens read their filters from the query string, so a page or a search is part of the
/// address and survives a reload. The detail screens take the identifier from the path and use the
/// pushed row only as the title placeholder shown before the data arrives.
GoRouter createRouter() => GoRouter(
  initialLocation: Routes.animes,
  routes: [
    ShellRoute(
      builder: (context, state, child) =>
          AdminShell(location: state.uri.path, child: child),
      routes: [
        GoRoute(
          path: Routes.animes,
          builder: (context, state) => ChangeNotifierProvider(
            create: (context) => AnimeListViewModel(
              context.read<AnimeRepository>(),
              search: state.uri.queryParameters['search'],
              page: int.tryParse(state.uri.queryParameters['page'] ?? '') ?? 1,
            ),
            child: const AnimeListView(),
          ),
          routes: [
            GoRoute(
              path: ':id',
              builder: (context, state) => ChangeNotifierProvider(
                create: (context) => AnimeDetailViewModel(
                  context.read<AnimeRepository>(),
                  context.read<EpisodeRepository>(),
                  state.pathParameters['id']!,
                  (state.extra as AnimeSummaryModel?)?.normalizedTitle ?? '',
                ),
                child: const AnimeDetailView(),
              ),
              routes: [
                GoRoute(
                  path: 'episodes/:episodeId',
                  builder: (context, state) => ChangeNotifierProvider(
                    create: (context) => EpisodeDetailViewModel(
                      context.read<EpisodeRepository>(),
                      state.pathParameters['episodeId']!,
                    ),
                    child: EpisodeDetailView(
                      episodeId: state.pathParameters['episodeId']!,
                      initialTitle: state.extra as String?,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
        GoRoute(
          path: Routes.episodes,
          builder: (context, state) => ChangeNotifierProvider(
            create: (context) =>
                LatestEpisodesViewModel(context.read<EpisodeRepository>()),
            child: const LatestEpisodesView(),
          ),
        ),
        GoRoute(
          path: Routes.settings,
          builder: (context, state) => const SettingsView(),
        ),
      ],
    ),
  ],
);
