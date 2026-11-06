import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/anime/anime_detail_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/anime_header.dart';

void main() {
  testWidgets('platform data stays visible without platform search actions', (
    tester,
  ) async {
    final anime = AnimeDetailModel.fromJson({
      'id': 'anime-id',
      'normalizedTitle': 'One Piece',
      'localizations': <dynamic>[],
      'images': <dynamic>[],
      'platforms': [
        {'platform': 'CRUNCHYROLL', 'platformId': '123'},
      ],
    });

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SingleChildScrollView(child: AnimeHeader(anime: anime)),
        ),
      ),
    );

    expect(find.text('Crunchyroll #123'), findsOneWidget);
    expect(find.byType(ActionChip), findsNothing);
    expect(find.byType(Chip), findsOneWidget);
  });
}
