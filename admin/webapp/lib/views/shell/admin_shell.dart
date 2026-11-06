import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/router/app_router.dart';

/// Frame of the administration: a sidebar on the left, the current screen on the right.
///
/// The sidebar is the same navigation as the former server-rendered administration: the catalog,
/// the recent releases and the settings are one click away, and the selected entry follows the
/// URL so a reload or a shared link highlights the right one.
class AdminShell extends StatelessWidget {
  const AdminShell({super.key, required this.location, required this.child});

  final String location;
  final Widget child;

  int get _selectedIndex {
    if (location.startsWith(Routes.episodes)) return 1;
    if (location.startsWith(Routes.settings)) return 2;

    return 0;
  }

  void _navigate(BuildContext context, int index) {
    context.go(switch (index) {
      1 => Routes.episodes,
      2 => Routes.settings,
      _ => Routes.animes,
    });
  }

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final theme = Theme.of(context);

    return Scaffold(
      body: Row(
        children: [
          NavigationRail(
            extended: true,
            selectedIndex: _selectedIndex,
            onDestinationSelected: (index) => _navigate(context, index),
            minExtendedWidth: 220,
            leading: Padding(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 28),
              child: Row(
                children: [
                  Icon(
                    Icons.play_circle_outline,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      localizations.appShortTitle,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            ),
            destinations: [
              NavigationRailDestination(
                icon: const Icon(Icons.movie_outlined),
                selectedIcon: const Icon(Icons.movie),
                label: Text(localizations.navigationAnimes),
              ),
              NavigationRailDestination(
                icon: const Icon(Icons.new_releases_outlined),
                selectedIcon: const Icon(Icons.new_releases),
                label: Text(localizations.navigationEpisodes),
              ),
              NavigationRailDestination(
                icon: const Icon(Icons.settings_outlined),
                selectedIcon: const Icon(Icons.settings),
                label: Text(localizations.settings),
              ),
            ],
          ),
          const VerticalDivider(width: 1),
          Expanded(child: child),
        ],
      ),
    );
  }
}
