import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'package:shikkanime_admin/core/network/http_client.dart';
import 'package:shikkanime_admin/core/theme/app_theme.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/repositories/anime_repository.dart';
import 'package:shikkanime_admin/repositories/episode_repository.dart';
import 'package:shikkanime_admin/router/app_router.dart';
import 'package:shikkanime_admin/viewmodels/settings_view_model.dart';

/// Starts the administration application.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final settings = SettingsViewModel();
  await settings.load();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: settings),
        Provider(
          create: (context) => HttpClient(context.read<SettingsViewModel>()),
        ),
        Provider(
          create: (context) => AnimeRepository(context.read<HttpClient>()),
        ),
        Provider(
          create: (context) => EpisodeRepository(context.read<HttpClient>()),
        ),
      ],
      child: const AdminApp(),
    ),
  );
}

/// Composition root of the administration interface.
class AdminApp extends StatelessWidget {
  const AdminApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp.router(
    title: 'Shikkanime — Administration',
    theme: AppTheme.light,
    darkTheme: AppTheme.dark,
    localizationsDelegates: const [
      ...GlobalMaterialLocalizations.delegates,
      ...AppLocalizations.localizationsDelegates,
    ],
    supportedLocales: AppLocalizations.supportedLocales,
    routerConfig: createRouter(),
    debugShowCheckedModeBanner: false,
  );
}
