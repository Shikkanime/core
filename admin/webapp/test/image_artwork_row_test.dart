import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shikkanime_admin/l10n/app_localizations.dart';
import 'package:shikkanime_admin/models/enums/image_type.dart';
import 'package:shikkanime_admin/models/image/image_model.dart';
import 'package:shikkanime_admin/views/anime_detail/widgets/image_artwork_row.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    supportedLocales: AppLocalizations.supportedLocales,
    home: Scaffold(body: child),
  );

  testWidgets(
    'shows the manual badge and the release action on a manual image',
    (tester) async {
      await tester.pumpWidget(
        wrap(
          ImageArtworkRow(
            image: const ImageModel(
              ImageType.THUMBNAIL,
              'https://a',
              manual: true,
            ),
            enabled: true,
            onSubmit: (_) async => null,
            onReset: () async => null,
          ),
        ),
      );

      expect(find.text('Manual'), findsOneWidget);
      expect(find.byIcon(Icons.undo), findsOneWidget);
      expect(find.byIcon(Icons.edit_outlined), findsOneWidget);
    },
  );

  testWidgets(
    'hides the manual badge and the release action on an automatic image',
    (tester) async {
      await tester.pumpWidget(
        wrap(
          ImageArtworkRow(
            image: const ImageModel(ImageType.BANNER, 'https://a'),
            enabled: true,
            onSubmit: (_) async => null,
            onReset: () async => null,
          ),
        ),
      );

      expect(find.text('Manual'), findsNothing);
      expect(find.byIcon(Icons.undo), findsNothing);
      expect(find.byIcon(Icons.edit_outlined), findsOneWidget);
    },
  );

  testWidgets('forwards the edited url to onSubmit', (tester) async {
    String? submitted;

    await tester.pumpWidget(
      wrap(
        ImageArtworkRow(
          image: const ImageModel(ImageType.THUMBNAIL, 'https://a'),
          enabled: true,
          onSubmit: (url) async {
            submitted = url;
            return null;
          },
          onReset: () async => null,
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.edit_outlined));
    await tester.pumpAndSettle();

    expect(find.text('Edit Thumbnail artwork'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'https://new');
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();

    expect(submitted, 'https://new');
    expect(find.text('Artwork updated'), findsOneWidget);
  });

  testWidgets('does not submit when the dialog is cancelled', (tester) async {
    var submitted = false;

    await tester.pumpWidget(
      wrap(
        ImageArtworkRow(
          image: const ImageModel(ImageType.TITLE, 'https://a'),
          enabled: true,
          onSubmit: (_) async {
            submitted = true;
            return null;
          },
          onReset: () async => null,
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.edit_outlined));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();

    expect(submitted, isFalse);
  });

  testWidgets('forwards the release to onReset', (tester) async {
    var reset = false;

    await tester.pumpWidget(
      wrap(
        ImageArtworkRow(
          image: const ImageModel(
            ImageType.THUMBNAIL,
            'https://a',
            manual: true,
          ),
          enabled: true,
          onSubmit: (_) async => null,
          onReset: () async {
            reset = true;
            return null;
          },
        ),
      ),
    );

    await tester.tap(find.byIcon(Icons.undo));
    await tester.pumpAndSettle();

    expect(reset, isTrue);
    expect(find.text('Artwork released to platform harvests'), findsOneWidget);
  });

  testWidgets('disables the actions while a mutation is in flight', (
    tester,
  ) async {
    await tester.pumpWidget(
      wrap(
        ImageArtworkRow(
          image: const ImageModel(ImageType.THUMBNAIL, 'https://a'),
          enabled: false,
          onSubmit: (_) async => null,
          onReset: () async => null,
        ),
      ),
    );

    final editButton = tester.widget<IconButton>(
      find.widgetWithIcon(IconButton, Icons.edit_outlined),
    );

    expect(editButton.onPressed, isNull);
  });
}
